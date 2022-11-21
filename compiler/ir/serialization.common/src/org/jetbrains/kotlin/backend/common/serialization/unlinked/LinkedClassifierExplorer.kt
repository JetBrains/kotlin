/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.LinkedClassifierStatus.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.LinkedClassifierStatus.Partially.*
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.isEnumEntry
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class LinkedClassifierExplorer(
    private val classifierSymbols: LinkedClassifierSymbols,
    private val stubGenerator: MissingDeclarationStubGenerator
) {

    fun exploreType(type: IrType): Partially? = type.exploreType(visitedSymbols = hashSetOf()) as? Partially
    fun exploreSymbol(symbol: IrClassifierSymbol): Partially? = symbol.exploreSymbol(visitedSymbols = hashSetOf()) as? Partially

    fun exploreIrElement(element: IrElement) {
        element.acceptChildrenVoid(IrElementExplorer { it.exploreType(visitedSymbols = hashSetOf()) })
    }

    /** Explore the IR type to find the first cause why this type should be considered as partially linked. */
    private fun IrType.exploreType(visitedSymbols: MutableSet<IrClassifierSymbol>): LinkedClassifierStatus {
        return when (this) {
            is IrSimpleType -> classifier.exploreSymbol(visitedSymbols) as? Partially
                ?: arguments.firstPartiallyLinkedStatus { (it as? IrTypeProjection)?.type?.exploreType(visitedSymbols) }
                ?: Fully
            is IrDynamicType -> Fully
            else -> throw IllegalArgumentException("Unsupported IR type: ${this::class.java}, $this")
        }
    }

    /** Explore the IR classifier symbol to find the first cause why this symbol should be considered as partially linked. */
    private fun IrClassifierSymbol.exploreSymbol(visitedSymbols: MutableSet<IrClassifierSymbol>): LinkedClassifierStatus {
        classifierSymbols[this]?.let { status ->
            // Already explored and registered symbol.
            return status
        }

        if (!isBound) {
            stubGenerator.getDeclaration(this) // Generate a stub and bind the symbol immediately.
            return classifierSymbols.registerPartiallyLinked(this, MissingClassifier(this))
        } else if ((owner as? IrLazyDeclarationBase)?.descriptor is NotFoundClasses.MockClassDescriptor) {
            // In case of Lazy IR the declaration is present, but wraps a special descriptor.
            return classifierSymbols.registerPartiallyLinked(this, MissingClassifier(this))
        }

        if (!visitedSymbols.add(this)) {
            return Fully // Recursion avoidance.
        }

        val dependencyStatus: Partially? = when (val classifier = owner) {
            is IrClass -> {
                if (classifier.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                    return classifierSymbols.registerPartiallyLinked(this, MissingClassifier(this))

                val parentStatus: Partially? = if (classifier.isInner || classifier.isEnumEntry) {
                    when (val parentClassSymbol = classifier.parentClassOrNull?.symbol) {
                        null -> return classifierSymbols.registerPartiallyLinked(this, MissingEnclosingClass(this as IrClassSymbol))
                        else -> parentClassSymbol.exploreSymbol(visitedSymbols) as? Partially
                    }
                } else
                    null

                parentStatus
                    ?: classifier.typeParameters.firstPartiallyLinkedStatus { it.symbol.exploreSymbol(visitedSymbols) }
                    ?: classifier.superTypes.firstPartiallyLinkedStatus { it.exploreType(visitedSymbols) }
            }

            is IrTypeParameter -> classifier.superTypes.firstPartiallyLinkedStatus { it.exploreType(visitedSymbols) }

            else -> null
        }

        val rootCause = when (dependencyStatus) {
            null -> return classifierSymbols.registerFullyLinked(this)
            is DueToOtherClassifier -> dependencyStatus.rootCause
            is CanBeRootCause -> dependencyStatus
        }

        return classifierSymbols.registerPartiallyLinked(this, DueToOtherClassifier(this, rootCause))
    }

    /** Iterate the collection and find the first partially linked status. */
    private inline fun <T> List<T>.firstPartiallyLinkedStatus(transform: (T) -> LinkedClassifierStatus?): Partially? =
        firstNotNullOfOrNull { transform(it) as? Partially }
}

private class IrElementExplorer(private val visitType: (IrType) -> Unit) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        visitType(declaration.type)
        super.visitValueParameter(declaration)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        declaration.superTypes.forEach(visitType)
        super.visitTypeParameter(declaration)
    }

    override fun visitFunction(declaration: IrFunction) {
        visitType(declaration.returnType)
        super.visitFunction(declaration)
    }

    override fun visitField(declaration: IrField) {
        visitType(declaration.type)
        super.visitField(declaration)
    }

    override fun visitVariable(declaration: IrVariable) {
        visitType(declaration.type)
        super.visitVariable(declaration)
    }

    override fun visitExpression(expression: IrExpression) {
        visitType(expression.type)
        super.visitExpression(expression)
    }

    override fun visitClassReference(expression: IrClassReference) {
        visitType(expression.classType)
        super.visitClassReference(expression)
    }

    override fun visitConstantObject(expression: IrConstantObject) {
        expression.typeArguments.forEach(visitType)
        super.visitConstantObject(expression)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        visitType(expression.typeOperand)
        super.visitTypeOperator(expression)
    }
}
