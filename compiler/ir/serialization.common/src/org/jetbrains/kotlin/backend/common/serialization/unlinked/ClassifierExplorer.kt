/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExploredClassifier.Unusable
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExploredClassifier.Unusable.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExploredClassifier.Usable
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.isEffectivelyMissingLazyIrDeclaration
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
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

internal class ClassifierExplorer(private val stubGenerator: MissingDeclarationStubGenerator) {
    private val exploredSymbols = ExploredClassifiers()

    fun exploreType(type: IrType): Unusable? = type.exploreType(visitedSymbols = hashSetOf()) as? Unusable
    fun exploreSymbol(symbol: IrClassifierSymbol): Unusable? = symbol.exploreSymbol(visitedSymbols = hashSetOf()) as? Unusable

    fun exploreIrElement(element: IrElement) {
        element.acceptChildrenVoid(IrElementExplorer { it.exploreType(visitedSymbols = hashSetOf()) })
    }

    /** Explore the IR type to find the first cause why this type should be considered as unusable. */
    private fun IrType.exploreType(visitedSymbols: MutableSet<IrClassifierSymbol>): ExploredClassifier {
        return when (this) {
            is IrSimpleType -> (classifier.exploreSymbol(visitedSymbols) as? Unusable)
                ?: arguments.firstUnusable { (it as? IrTypeProjection)?.type?.exploreType(visitedSymbols) }
                ?: Usable
            is IrDynamicType -> Usable
            else -> throw IllegalArgumentException("Unsupported IR type: ${this::class.java}, $this")
        }
    }

    /** Explore the IR classifier symbol to find the first cause why this symbol should be considered as unusable. */
    private fun IrClassifierSymbol.exploreSymbol(visitedSymbols: MutableSet<IrClassifierSymbol>): ExploredClassifier {
        exploredSymbols[this]?.let { result ->
            // Already explored and registered symbol.
            return result
        }

        if (!isBound) {
            stubGenerator.getDeclaration(this) // Generate a stub and bind the symbol immediately.
            return exploredSymbols.registerUnusable(this, MissingClassifier(this))
        }

        (owner as? IrLazyClass)?.let { lazyIrClass ->
            val isEffectivelyMissingClassifier =
                /* Lazy IR declaration is present but wraps a special "not found" class descriptor. */
                lazyIrClass.descriptor is NotFoundClasses.MockClassDescriptor
                        /* The outermost class containing the lazy IR declaration is private, which normally should not happen
                         * because the declaration is exported from the module. */
                        || lazyIrClass.isEffectivelyMissingLazyIrDeclaration()

            if (isEffectivelyMissingClassifier)
                return exploredSymbols.registerUnusable(this, MissingClassifier(this))
        }

        if (!visitedSymbols.add(this)) {
            return Usable // Recursion avoidance.
        }

        val cause: Unusable? = when (val classifier = owner) {
            is IrClass -> {
                if (classifier.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                    return exploredSymbols.registerUnusable(this, MissingClassifier(this))

                val outerClassSymbol = if (classifier.isInner || classifier.isEnumEntry) {
                    classifier.parentClassOrNull?.symbol
                        ?: return exploredSymbols.registerUnusable(this, MissingEnclosingClass(this as IrClassSymbol))
                } else null

                (outerClassSymbol?.exploreSymbol(visitedSymbols) as? Unusable)
                    ?: classifier.typeParameters.firstUnusable { it.symbol.exploreSymbol(visitedSymbols) }
                    ?: classifier.superTypes.firstUnusable { it.exploreType(visitedSymbols) }
            }

            is IrTypeParameter -> classifier.superTypes.firstUnusable { it.exploreType(visitedSymbols) }

            else -> null
        }

        val rootCause = when (cause) {
            null -> return exploredSymbols.registerUsable(this)
            is DueToOtherClassifier -> cause.rootCause
            is CanBeRootCause -> cause
        }

        return exploredSymbols.registerUnusable(this, DueToOtherClassifier(this, rootCause))
    }

    /** Iterate the collection and find the first unusable classifier. */
    private inline fun <T> List<T>.firstUnusable(transform: (T) -> ExploredClassifier?): Unusable? =
        firstNotNullOfOrNull { transform(it) as? Unusable }
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
