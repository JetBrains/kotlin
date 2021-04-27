/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid


interface IdSignatureClashTracker {
    fun commit(declaration: IrDeclaration, signature: IdSignature)

    companion object {
        val DEFAULT_TRACKER = object : IdSignatureClashTracker {
            override fun commit(declaration: IrDeclaration, signature: IdSignature) {}
        }
    }
}

abstract class GlobalDeclarationTable(
    val signaturer: IdSignatureSerializer,
    private val mangler: KotlinMangler.IrMangler,
    private val clashTracker: IdSignatureClashTracker
) {
    protected val table = mutableMapOf<IrDeclaration, IdSignature>()

    constructor(signaturer: IdSignatureSerializer, mangler: KotlinMangler.IrMangler) :
            this(signaturer, mangler, IdSignatureClashTracker.DEFAULT_TRACKER)

    protected fun loadKnownBuiltins(builtIns: IrBuiltIns) {
        builtIns.knownBuiltins.forEach {
            val symbol = (it as IrSymbolOwner).symbol
            table[it] = symbol.signature!!.also { id -> clashTracker.commit(it, id) }
        }
    }

    open fun computeSignatureByDeclaration(declaration: IrDeclaration): IdSignature {
        return table.getOrPut(declaration) {
            signaturer.composePublicIdSignature(declaration).also { clashTracker.commit(declaration, it) }
        }
    }

    fun isExportedDeclaration(declaration: IrDeclaration, compatibleMode: Boolean): Boolean = with(mangler) { declaration.isExported(compatibleMode) }
}

open class DeclarationTable(globalTable: GlobalDeclarationTable) {
    protected val table = mutableMapOf<IrDeclaration, IdSignature>()
    protected open val globalDeclarationTable: GlobalDeclarationTable = globalTable
    // TODO: we need to disentangle signature construction with declaration tables.
    private val signaturer: IdSignatureSerializer = globalTable.signaturer.also {
//        it.reset()
        it.table = this
    }

    fun inFile(file: IrFile, block: () -> Unit) {
        signaturer.reset()
        signaturer.table = this
        signaturer.inFile(file.symbol, block)
    }


    private class LocalScopeBuilder : ScopeBuilder<IrDeclaration, IrElement> {
        private class LocalIndexCollector(private val scope: SignatureScope<IrDeclaration>) : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                if (declaration.kind == ClassKind.OBJECT) {
                    // TODO: is that correct?
                    scope.commitAnonymousObject(declaration)
                } else {
                    scope.commitLocalClass(declaration)
                }
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                if (declaration.visibility == DescriptorVisibilities.LOCAL) {
                    scope.commitLocalFunction(declaration)
                }
            }

            override fun visitFunctionExpression(expression: IrFunctionExpression) {
                scope.commitLambda(expression.function)
            }

            override fun visitEnumEntry(declaration: IrEnumEntry) {
                if (declaration.name.asString() == "BASIC_LATIN") {
                    println("X")
                }
                super.visitEnumEntry(declaration)
            }

        }

        override fun build(scope: SignatureScope<IrDeclaration>, element: IrElement?) {
            element?.acceptChildrenVoid(LocalIndexCollector(scope))
        }
    }

    fun <R> inLocalScope(scopeOwner: IrElement, block: () -> R): R {
        return signaturer.inLocalScope({ LocalScopeBuilder().build(it, scopeOwner) }, block)
    }

    private fun IrDeclaration.isLocalDeclaration(compatibleMode: Boolean): Boolean {
        return !isExportedDeclaration(this, compatibleMode)
    }

    fun isExportedDeclaration(declaration: IrDeclaration, compatibleMode: Boolean) =
        globalDeclarationTable.isExportedDeclaration(declaration, compatibleMode)

    protected open fun tryComputeBackendSpecificSignature(declaration: IrDeclaration): IdSignature? = null

    private fun allocateIndexedSignature(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        return table.getOrPut(declaration) { signaturer.composeFileLocalIdSignature(declaration, compatibleMode) }
    }

    private fun computeSignatureByDeclaration(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        tryComputeBackendSpecificSignature(declaration)?.let { return it }
        return if (declaration.isLocalDeclaration(compatibleMode)) {
            allocateIndexedSignature(declaration, compatibleMode)
        } else globalDeclarationTable.computeSignatureByDeclaration(declaration)
    }

    fun privateDeclarationSignature(declaration: IrDeclaration, compatibleMode: Boolean, builder: () -> IdSignature): IdSignature {
        assert(declaration.isLocalDeclaration(compatibleMode))
        return table.getOrPut(declaration) { builder() }
    }

    fun signatureByDeclaration(declaration: IrDeclaration, compatibleMode: Boolean): IdSignature {
        return computeSignatureByDeclaration(declaration, compatibleMode)
    }

    fun assumeDeclarationSignature(declaration: IrDeclaration, signature: IdSignature) {
        assert(table[declaration] == null) { "Declaration table already has signature for ${declaration.render()}" }
        table.put(declaration, signature)
    }
}

// This is what we pre-populate tables with
val IrBuiltIns.knownBuiltins: List<IrDeclaration>
    get() = packageFragment.declarations
