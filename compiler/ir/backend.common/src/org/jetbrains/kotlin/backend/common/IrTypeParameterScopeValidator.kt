/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrTypeTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Makes sure that all the type parameter references are within the scope of the corresponding type parameters.
 */
internal class IrTypeParameterScopeValidator(private val reportError: ReportError, private val parentChain: MutableList<IrElement>) {

    fun check(element: IrElement) {
        element.acceptVoid(Checker())
    }

    private inner class Checker : IrTypeTransformerVoid() {
        private val scopeStack = ScopeStack<IrTypeParameterSymbol>()

        private fun checkTypeParameterReference(element: IrElement, typeParameterSymbol: IrTypeParameterSymbol) {
            if (!scopeStack.isVisibleInCurrentScope(typeParameterSymbol)) {
                reportError(
                    element,
                    "The following element references a type parameter '${typeParameterSymbol.owner.render()}' that is not available " +
                            "in the current scope."
                )
            }
        }

        private fun visitTypeAccess(element: IrElement, type: IrType) {
            if (type !is IrSimpleType) return
            (type.classifier as? IrTypeParameterSymbol)?.let {
                checkTypeParameterReference(element, it)
            }
            for (arg in type.arguments) {
                if (arg is IrTypeProjection) {
                    visitTypeAccess(element, arg.type)
                }
            }
        }

        private fun withTypeParametersInScope(container: IrTypeParametersContainer, block: () -> Unit) {
            scopeStack.withNewScope(
                outerScopesAreInvisible = container is IrClass && !container.isInner && container.visibility != DescriptorVisibilities.LOCAL,
                populateScope = { container.typeParameters.forEach { add(it.symbol) } },
                block = block,
            )
        }

        override fun <Type : IrType?> transformType(container: IrElement, type: Type): Type {
            if (type != null) {
                visitTypeAccess(container, type)
            }
            return type
        }

        override fun visitElement(element: IrElement) {
            parentChain.temporarilyPushing(element) {
                element.acceptChildrenVoid(this)
            }
        }

        override fun visitFunction(declaration: IrFunction) {
            withTypeParametersInScope(declaration) {
                super.visitFunction(declaration)
            }
        }

        override fun visitClass(declaration: IrClass) {
            withTypeParametersInScope(declaration) {
                super.visitClass(declaration)
            }
        }

        override fun visitTypeAlias(declaration: IrTypeAlias) {
            withTypeParametersInScope(declaration) {
                super.visitTypeAlias(declaration)
            }
        }
    }
}