/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import gnu.trove.THashMap
import org.jetbrains.kotlin.backend.common.serialization.unlinked.LinkedClassifierStatus.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
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

internal class LinkedClassifierExplorer(private val stubGenerator: MissingDeclarationStubGenerator) {
    private val classifierSymbols = THashMap<IrClassifierSymbol, LinkedClassifierStatus>()

    fun exploreType(type: IrType): LinkedClassifierStatus = type.exploreType(visitedSymbols = hashSetOf(), statusBuilder = null)
    fun exploreSymbol(symbol: IrClassifierSymbol): LinkedClassifierStatus = symbol.exploreSymbol(visitedSymbols = hashSetOf())

    fun exploreIrElement(element: IrElement) {
        element.acceptChildrenVoid(IrElementExplorer { it.exploreType(visitedSymbols = hashSetOf()) })
    }

    /** Explore the IR type to find the first cause why this type should be considered as partially linked. */
    private fun IrType.exploreType(visitedSymbols: MutableSet<IrClassifierSymbol>, statusBuilder: StatusBuilder?): LinkedClassifierStatus {
        return when (this) {
            is IrSimpleType -> {
                val symbolStatus = classifier.exploreSymbol(visitedSymbols)
                statusBuilder?.refineStatus(symbolStatus)
                if (symbolStatus is Partially)
                    return symbolStatus

                arguments.forEach { argument ->
                    val type = (argument as? IrTypeProjection)?.type ?: return@forEach
                    val typeStatus = type.exploreType(visitedSymbols, statusBuilder)
                    if (typeStatus is Partially)
                        return typeStatus
                }

                classifier.exploreSymbol(visitedSymbols) as? Partially
                    ?: arguments.firstPartiallyLinkedStatus { (it as? IrTypeProjection)?.type?.exploreType(visitedSymbols) }
                    ?: Fully
            }
            is IrDynamicType -> NoClassifier
            else -> throw IllegalArgumentException("Unsupported IR type: ${this::class.java}, $this")
        }
    }

    /** Explore the IR classifier symbol to find the first cause why this symbol should be considered as partially linked. */
    private fun IrClassifierSymbol.exploreSymbol(visitedSymbols: MutableSet<IrClassifierSymbol>): LinkedClassifierStatus {
        // TODO: check filters
//        if (this.isTrusted()) {
//            return Fully.TrustedClassifier
//        }

        classifierSymbols[this]?.let { status ->
            // Already explored and registered symbol.
            return status
        }

        if (!isBound) {
            stubGenerator.getDeclaration(this) // Generate a stub and bind the symbol immediately.
            return registerStatus(this, Partially.MissingClassifier(this))
        } else if ((owner as? IrLazyDeclarationBase)?.descriptor is NotFoundClasses.MockClassDescriptor) {
            // In case of Lazy IR the declaration is present, but wraps a special descriptor.
            return registerStatus(this, Partially.MissingClassifier(this))
        }

        if (!visitedSymbols.add(this)) {
            return RecursionAvoidance // Recursion avoidance.
        }

        val statusBuilder = StatusBuilder(this)

        when (val classifier = owner) {
            is IrClass -> {
                if (classifier.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                    return registerStatus(this, Partially.MissingClassifier(this))

                if (classifier.isInner || classifier.isEnumEntry) {
                    when (val parentClassSymbol = classifier.parentClassOrNull?.symbol) {
                        null -> return registerStatus(this, Partially.MissingEnclosingClass(this as IrClassSymbol))
                        else -> statusBuilder.refineStatus { parentClassSymbol.exploreSymbol(visitedSymbols) }
                    }
                }

                statusBuilder.refineStatus(classifier.typeParameters) { it.symbol.exploreSymbol(visitedSymbols) }
                statusBuilder.refineStatus(classifier.superTypes) { it.exploreType(visitedSymbols) }
            }

            is IrTypeParameter -> statusBuilder.refineStatus(classifier.superTypes) { it.exploreType(visitedSymbols) }
        }

        return registerStatus(this, statusBuilder.toStatus())
    }

    /** Iterate the collection and find the first partially linked status. */
    private inline fun <T> List<T>.firstPartiallyLinkedStatus(transform: (T) -> LinkedClassifierStatus?): Partially? =
        firstNotNullOfOrNull { transform(it) as? Partially }

    private fun <S : LinkedClassifierStatus> registerStatus(symbol: IrClassifierSymbol, status: S): S {
        classifierSymbols[symbol] = status
        return status
    }
}

private class StatusBuilder(private val symbol: IrClassifierSymbol) {
    private val ownLimitation = computeOwnLimitation(symbol)
    private var dependencyWithNarrowerLimitation: Fully.AccessibleClassifier? = null

    private var partiallyLinkedStatus: Partially? = null
    private val done get() = partiallyLinkedStatus != null

    fun refineStatus(status: LinkedClassifierStatus) {
        if (!done) consumeStatus(status)
    }

    fun refineStatus(block: () -> LinkedClassifierStatus) {
        if (!done) consumeStatus(block())
    }

    fun <T> refineStatus(collection: Collection<T>, block: (T) -> LinkedClassifierStatus) {
        if (!done && collection.isNotEmpty()) {
            collection.asSequence().map(block).forEach { next ->
                consumeStatus(next)
                if (done) return
            }
        }
    }

    private fun consumeStatus(next: LinkedClassifierStatus) {
        when (next) {
            is Partially.CanBeRootCause -> partiallyLinkedStatus = Partially.DueToOtherClassifier(symbol, next)
            is Partially.DueToOtherClassifier -> partiallyLinkedStatus = Partially.DueToOtherClassifier(symbol, next.rootCause)

            is Fully -> {
                val nextAccessible: Fully.AccessibleClassifier = when (next) {
                    is Fully.AccessibleClassifier -> next
                    is Fully.LesserAccessibleClassifier -> next.dueTo
                }

                fun checkDependency(currentLimitedTo: LimitedTo, onConflictingLimitations: (LimitedTo.CanBeRootCause) -> Partially) {
                    val nextLimitedTo = nextAccessible.limitation

                    when (currentLimitedTo) {
                        is LimitedTo.WholeWorld -> if (nextLimitedTo !is LimitedTo.WholeWorld)
                            this.dependencyWithNarrowerLimitation = nextAccessible

                        is LimitedTo.CanBeRootCause -> if (nextLimitedTo is LimitedTo.CanBeRootCause)
                            when (chooseNarrower(currentLimitedTo, nextLimitedTo)) {
                                null -> partiallyLinkedStatus = onConflictingLimitations(currentLimitedTo)
                                currentLimitedTo -> Unit
                                else -> this.dependencyWithNarrowerLimitation = nextAccessible
                            }
                    }
                }

                when (val dependencyWithNarrowerLimitation = dependencyWithNarrowerLimitation) {
                    null -> {
                        // Compare the own limitation of the classifier with the limitation of the `next` dependency.
                        checkDependency(ownLimitation) { currentLimitedTo ->
                            Partially.InaccessibleClassifier(symbol, currentLimitedTo, nextAccessible)
                        }
                    }

                    else -> {
                        // Compare the limitation of the latest memoized dependency with the limitation of the `next` dependency.
                        checkDependency(dependencyWithNarrowerLimitation.limitation) {
                            Partially.InaccessibleClassifierDueToOtherClassifiers(symbol, dependencyWithNarrowerLimitation, nextAccessible)
                        }
                    }
                }
            }

            is RecursionAvoidance, is NoClassifier -> return // Just skip them.
        }
    }

    fun toStatus(): LinkedClassifierStatus {
        return partiallyLinkedStatus
            ?: dependencyWithNarrowerLimitation?.let { Fully.LesserAccessibleClassifier(symbol, it) }
            ?: Fully.AccessibleClassifier(symbol, ownLimitation)
    }

    companion object {
        private fun computeOwnLimitation(symbol: IrClassifierSymbol): LimitedTo {
            val declaration = symbol.owner as? IrDeclarationWithVisibility
                ?: return LimitedTo.WholeWorld // Type parameters do not have own visibility.

            return when (declaration.visibility) {
                DescriptorVisibilities.PUBLIC, DescriptorVisibilities.PROTECTED -> LimitedTo.WholeWorld
                DescriptorVisibilities.INTERNAL -> LimitedTo.Module(PartialLinkageUtils.Module.determineFor(declaration))
                else -> LimitedTo.File(PartialLinkageUtils.File.determineFor(declaration))
            }
        }

        private fun chooseNarrower(limit1: LimitedTo.CanBeRootCause, limit2: LimitedTo.CanBeRootCause): LimitedTo.CanBeRootCause? {
            return when (limit1) {
                is LimitedTo.Module -> when (limit2) {
                    is LimitedTo.Module -> if (limit1.module == limit2.module) limit2 else null
                    is LimitedTo.File -> if (limit1.module == limit2.file.module) limit2 else null
                }
                is LimitedTo.File -> when (limit2) {
                    is LimitedTo.Module -> if (limit1.file.module == limit2.module) limit1 else null
                    is LimitedTo.File -> if (limit1.file == limit2.file) limit1 else null
                }
            }
        }
    }
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
