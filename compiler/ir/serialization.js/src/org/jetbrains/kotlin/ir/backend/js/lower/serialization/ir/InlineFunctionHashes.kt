/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.NonLinkingIrInlineFunctionDeserializer
import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.incremental.js.InlineFunctionSymbol
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import java.nio.ByteBuffer

/**
 * Captures inline implementations before pre-serialization lowerings replace calls with copies of their bodies.
 * An implementation includes bodies of transitively called inline functions and referenced constants. Only the
 * resulting hashes are persisted by IC; the dependency information below belongs to this compilation round.
 *
 * Overloads and property accessors share a source lookup name, so their hashes are combined under the same key.
 */
fun collectInlineFunctionHashes(
    module: IrModuleFragment,
    inlineFunctionDeserializer: NonLinkingIrInlineFunctionDeserializer? = null,
): Map<IrFile, Map<InlineFunctionSymbol, Long>> {
    val calculator = InlineFunctionHashCalculator(inlineFunctionDeserializer)
    return module.files.associateWith { file ->
        val hashesBySymbol = linkedMapOf<InlineFunctionSymbol, MutableList<Long>>()
        file.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                // Local declarations are already included in the hash of the enclosing inline function.
                if (declaration !is IrSimpleFunction || !declaration.isInline || declaration.isFakeOverride) return
                val lookupDeclaration = declaration.correspondingPropertySymbol?.owner ?: declaration
                val fqName = lookupDeclaration.fqNameWhenAvailable ?: return
                val symbol = InlineFunctionSymbol(fqName.parent().asString(), lookupDeclaration.name.asString())
                hashesBySymbol.getOrPut(symbol) { mutableListOf() }.add(calculator.calculateHash(declaration))
            }
        })
        hashesBySymbol.mapValues { entry -> combineHashes(entry.value.sorted()) }
    }
}

private class InlineFunctionHashCalculator(
    private val inlineFunctionDeserializer: NonLinkingIrInlineFunctionDeserializer?,
) {
    private class Dependencies(
        val inlineFunctions: Set<IrSimpleFunction>,
        val constants: Set<IrProperty>,
    )

    private val flatHashes = hashMapOf<IrDeclarationWithName, Long>()
    private val dependencies = hashMapOf<IrSimpleFunction, Dependencies>()
    private val deserializedFunctions = hashMapOf<IrSimpleFunction, IrSimpleFunction?>()

    private val dumpOptions = DumpIrTreeOptions(
        normalizeNames = true,
        stableOrderOfOverriddenSymbols = true,
        renderOriginForExternalDeclarations = false,
        printModuleName = false,
        printFilePath = false,
        printSourceOffsets = false,
        filePathRenderer = { _, _ -> "" },
        referenceRenderingStrategy = DumpIrTreeOptions.ReferenceRenderingStrategy.Custom { symbol ->
            // Prepared inline bodies can refer to unbound symbols. The default dump loses their identities.
            if (symbol.isBound) null else symbol.signature?.render()
        },
    )

    private fun IrDeclarationWithName.flatHash(): Long = flatHashes.getOrPut(this) {
        val original = (this as? IrSimpleFunction)?.originalOfPreparedInlineFunctionCopy ?: this
        val representation = buildString {
            // Keep a dependency's identity attached to its body when combining component hashes. Otherwise,
            // swapping the implementations of two dependencies could leave the caller's hash unchanged.
            append(original.fqNameWhenAvailable?.asString())
            append('\n')
            append(dumpOrFail(dumpOptions))
        }
        cityHash64(representation.toByteArray()).toLong()
    }

    private fun IrSimpleFunction.implementation(): IrSimpleFunction? {
        val function = resolveFakeOverrideOrFail()
        if (function.body != null) return function
        if (inlineFunctionDeserializer == null || function.isEffectivelyPrivate() ||
            function.containerSource !is KlibDeserializedContainerSource
        ) return null

        return deserializedFunctions.getOrPut(function) {
            inlineFunctionDeserializer.deserializeInlineFunction(function)
        }
    }

    private fun IrSimpleFunction.dependencies(): Dependencies = dependencies.getOrPut(this) {
        val inlineFunctions = linkedSetOf<IrSimpleFunction>()
        val constants = linkedSetOf<IrProperty>()
        acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                if (expression.symbol.isBound) {
                    val callee = expression.symbol.owner
                    if (callee.isInline) callee.implementation()?.let(inlineFunctions::add)
                    callee.correspondingPropertySymbol?.owner?.takeIf { it.isConst }?.let(constants::add)
                }
                expression.acceptChildrenVoid(this)
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                if (expression.symbol.isBound) {
                    val function = expression.symbol.owner
                    if (function is IrSimpleFunction && function.isInline && !function.isFakeOverride) {
                        function.implementation()?.let(inlineFunctions::add)
                    }
                }
                expression.acceptChildrenVoid(this)
            }

            override fun visitGetField(expression: IrGetField) {
                if (expression.symbol.isBound) {
                    expression.symbol.owner.correspondingPropertySymbol?.owner?.takeIf { it.isConst }?.let(constants::add)
                }
                expression.acceptChildrenVoid(this)
            }
        })
        Dependencies(inlineFunctions, constants)
    }

    fun calculateHash(function: IrSimpleFunction): Long {
        val visitedFunctions = hashSetOf(function)
        val visitedConstants = hashSetOf<IrProperty>()
        val pendingFunctions = mutableListOf(function)
        val hashes = mutableListOf<Long>()
        while (pendingFunctions.isNotEmpty()) {
            val current = pendingFunctions.removeLast()
            hashes.add(current.flatHash())
            val currentDependencies = current.dependencies()
            for (dependency in currentDependencies.inlineFunctions) {
                if (visitedFunctions.add(dependency)) pendingFunctions.add(dependency)
            }
            for (constant in currentDependencies.constants) {
                if (visitedConstants.add(constant)) {
                    hashes.add(constant.flatHash())
                }
            }
        }
        // Preserve dependency positions: different private declarations can have the same fully qualified name.
        // Sorting here could hide a change that swaps their bodies. Traversal order is determined by the order of calls in IR.
        return combineHashes(hashes)
    }
}

private fun combineHashes(hashes: List<Long>): Long {
    val bytes = ByteBuffer.allocate(hashes.size * Long.SIZE_BYTES)
    hashes.forEach(bytes::putLong)
    return cityHash64(bytes.array()).toLong()
}
