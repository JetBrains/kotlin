/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.inline.KlibSyntheticAccessorGenerator
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Generates synthetic accessor functions for private declarations that are referenced from non-private inline functions,
 * so that after those functions are inlined, there'll be no visibility violations.
 *
 * There are a few important assumptions that this lowering relies on:
 * - It's executed in a KLIB-based backend. It's not designed to work with the JVM backend because the visibility rules on JVM are
 *   stricter.
 * - By the point it's executed, all _private_ inline functions have already been inlined.
 */
class SyntheticAccessorLowering(context: CommonBackendContext) : FileLoweringPass {
    private data class GeneratedAccessor(
        val accessor: IrFunction,
        val targetSymbol: IrSymbol,
    )

    private class GeneratedAccessors {
        private val accessors = HashSet<GeneratedAccessor>()
        private var frozen = false

        operator fun plusAssign(accessor: GeneratedAccessor) {
            check(!frozen) { "An attempt to generate an accessor after all accessors have been already added to their containers" }
            accessors += accessor
        }

        fun freezeAndGetAccessors(): Set<GeneratedAccessor> {
            check(!frozen) { "An attempt to add the generated accessors to their containers once again" }
            frozen = true
            return accessors
        }
    }

    private val accessorGenerator = KlibSyntheticAccessorGenerator(context)

    override fun lower(irFile: IrFile) {
        val transformer = Transformer(irFile)
        irFile.transformChildrenVoid(transformer)

        addAccessorsToParents(transformer.generatedAccessors)
    }

    fun lowerWithoutAddingAccessorsToParents(irFunction: IrFunction) {
        irFunction.accept(Transformer(irFunction.file), null)
    }

    /**
     * Add accessors to their containers repeating the order of the corresponding target declarations.
     * Example:
     * ```
     *   class MyClass
     *     // declarations that are used inside inline function:
     *     private val foo
     *       private get()
     *       private set(value)
     *     public val bar:
     *       public get()
     *       private set(value)
     *     public fun baz()
     *     private fun qux()
     *     // their accessors in the "proper" order:
     *     fun access$<get-foo>()
     *     fun access$<set-foo>()
     *     fun access$<set-bar>()
     *     fun access$qux()
     * ```
     */
    private fun addAccessorsToParents(generatedAccessors: GeneratedAccessors) {
        val accessors = generatedAccessors.freezeAndGetAccessors()
        if (accessors.isEmpty()) return

        for ((parent, accessorsInParent) in accessors.groupBy { it.accessor.parent }) {
            addAccessorsToParent(parent as IrDeclarationContainer, accessorsInParent)
        }
    }

    private fun addAccessorsToParent(parent: IrDeclarationContainer, accessors: List<GeneratedAccessor>) {
        if (accessors.size == 1) {
            parent.declarations += accessors[0].accessor
            return
        }

        val targetToAccessor: Map<IrDeclaration, IrFunction> = accessors.associate { accessor ->
            accessor.targetSymbol.owner as IrDeclaration to accessor.accessor
        }

        val remainingAccessors: MutableSet<IrFunction> = accessors.mapTo(HashSet(), GeneratedAccessor::accessor)

        fun addAccessorToParent(maybeTarget: IrDeclaration?) {
            if (maybeTarget == null) return
            val accessor = targetToAccessor[maybeTarget] ?: return
            parent.declarations += accessor
            remainingAccessors -= accessor
        }

        for (maybeTarget in parent.declarations.toList()) {
            addAccessorToParent(maybeTarget)
            if (maybeTarget is IrProperty) {
                addAccessorToParent(maybeTarget.getter)
                addAccessorToParent(maybeTarget.setter)
            }
        }

        parent.declarations += remainingAccessors // unexpected, but...
    }

    private inner class Transformer(irFile: IrFile) : IrElementTransformerVoid() {
        val generatedAccessors = irFile.generatedAccessors ?: GeneratedAccessors().also { irFile.generatedAccessors = it }

        private var currentInlineFunction: IrFunction? = null
        private var insideBody = false

        override fun visitFunction(declaration: IrFunction): IrStatement {
            val previousInlineFunction = currentInlineFunction
            try {
                currentInlineFunction = if (declaration.isInline) {
                    declaration.takeIf {
                        // By the time this lowering is executed, there must be no private inline functions, however, there are exceptions, for example,
                        // suspendCoroutineUninterceptedOrReturn, which are somewhat magical.
                        // If we encounter one, just ignore it.
                        !declaration.isConsideredAsPrivateForInlining()
                    }
                } else {
                    previousInlineFunction
                }

                return declaration.factory.stageController.restrictTo(declaration) {
                    super.visitFunction(declaration)
                }
            } finally {
                currentInlineFunction = previousInlineFunction
            }
        }

        override fun visitBody(body: IrBody): IrBody {
            val previousInsideBody = insideBody
            try {
                insideBody = true
                return super.visitBody(body)
            } finally {
                insideBody = previousInsideBody
            }
        }

        override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
            if (currentInlineFunction == null || !insideBody || !expression.symbol.owner.isAbiPrivate)
                return super.visitFunctionAccess(expression)

            // TODO(KT-69527): Set the proper visibility for the accessor (the max visibility of all the inline functions that reference it)
            val accessor = accessorGenerator.getSyntheticFunctionAccessor(expression, null)
            val accessorExpression = accessorGenerator.modifyFunctionAccessExpression(expression, accessor.symbol)

            generatedAccessors += GeneratedAccessor(accessor, expression.symbol)

            return super.visitFunctionAccess(accessorExpression)
        }
    }

    companion object {
        private var IrFile.generatedAccessors: GeneratedAccessors? by irAttribute(followAttributeOwner = false)

        // TODO: Take into account visibilities of containers
        // TODO(KT-69565): It's not enough to just look at the visibility, since the declaration may be private inside a local class
        //   and accessed only within that class. For such cases we shouldn't generate an accessor.
        private val IrDeclarationWithVisibility.isAbiPrivate: Boolean
            get() = DescriptorVisibilities.isPrivate(visibility) || visibility == DescriptorVisibilities.LOCAL
    }
}
