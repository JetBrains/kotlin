/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import gnu.trove.THashMap
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ABIVisibility.Companion.chooseNarrower
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
    private val exploredSymbols = THashMap<IrClassifierSymbol, ExploredClassifier>()

    fun exploreType(type: IrType): TypeExplorationResult = type.exploreType(visitedSymbols = hashSetOf())
    fun exploreSymbol(symbol: IrClassifierSymbol): ExploredClassifier = symbol.exploreSymbol(visitedSymbols = hashSetOf())

    fun exploreIrElement(element: IrElement) {
        element.acceptChildrenVoid(IrElementExplorer { it.exploreType(visitedSymbols = hashSetOf()) })
    }

    /** Explore the IR type to find the first cause why this type should be considered as partially linked. */
    private fun IrType.exploreType(visitedSymbols: MutableSet<IrClassifierSymbol>): TypeExplorationResult {
        return when (this) {
            is IrSimpleType -> typeExploration {
                exploreByClassifier { classifier.exploreSymbol(visitedSymbols) }
                exploreByTypes(arguments) { argument -> (argument as? IrTypeProjection)?.type?.exploreType(visitedSymbols) }
            }
            is IrDynamicType -> TypeExplorationResult.UsableType.DEFAULT_PUBLIC
            else -> throw IllegalArgumentException("Unsupported IR type: ${this::class.java}, $this")
        }
    }

    /** Explore the IR classifier symbol to find the first cause why this symbol should be considered as partially linked. */
    private fun IrClassifierSymbol.exploreSymbol(visitedSymbols: MutableSet<IrClassifierSymbol>): ExploredClassifier {
        // TODO: check filters
//        if (this.isTrusted()) {
//            return Fully.TrustedClassifier
//        }

        exploredSymbols[this]?.let { result ->
            // Already explored and registered symbol.
            return result
        }

        if (!isBound) {
            stubGenerator.getDeclaration(this) // Generate a stub and bind the symbol immediately.
            return registerClassifier(this, ExploredClassifier.Unusable.MissingClassifier(this))
        } else if ((owner as? IrLazyDeclarationBase)?.descriptor is NotFoundClasses.MockClassDescriptor) {
            // In case of Lazy IR the declaration is present, but wraps a special descriptor.
            return registerClassifier(this, ExploredClassifier.Unusable.MissingClassifier(this))
        }

        if (!visitedSymbols.add(this)) {
            return ExploredClassifier.RecursionAvoidance
        }

        val explorationResult = when (val classifier = owner) {
            is IrClass -> {
                if (classifier.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
                    return registerClassifier(this, ExploredClassifier.Unusable.MissingClassifier(this))

                val outerClassSymbol = if (classifier.isInner || classifier.isEnumEntry) {
                    classifier.parentClassOrNull?.symbol
                        ?: return registerClassifier(this, ExploredClassifier.Unusable.MissingEnclosingClass(this as IrClassSymbol))
                } else null

                classifierExploration {
                    if (outerClassSymbol != null) exploreByClassifier { outerClassSymbol.exploreSymbol(visitedSymbols) }
                    exploreByClassifiers(classifier.typeParameters) { it.symbol.exploreSymbol(visitedSymbols) }
                    exploreByTypes(classifier.superTypes) { it.exploreType(visitedSymbols) }
                }
            }

            is IrTypeParameter -> classifierExploration {
                exploreByTypes(classifier.superTypes) { it.exploreType(visitedSymbols) }
            }

            else -> throw IllegalArgumentException("Unsupported IR classifier: ${this::class.java}, $this")
        }

        return registerClassifier(this, explorationResult)
    }

    private fun <S : ExploredClassifier> registerClassifier(symbol: IrClassifierSymbol, explorationResult: S): S {
        exploredSymbols[symbol] = explorationResult
        return explorationResult
    }
}

private abstract class ExplorationResultBuilder<R, UR : R> {
    protected var dependencyWithNarrowerVisibility: ExploredClassifier.Usable.AccessibleClassifier? = null

    protected var unusableResult: UR? = null
    private inline val done get() = unusableResult != null

    fun exploreByClassifier(block: () -> ExploredClassifier) {
        if (!done) consume(block())
    }

    fun exploreByType(block: () -> TypeExplorationResult) {
        if (!done) consume(block())
    }

    fun <T> exploreByClassifiers(collection: Collection<T>, block: (T) -> ExploredClassifier?) {
        if (!done && collection.isNotEmpty()) {
            collection.asSequence().mapNotNull(block).forEach { next ->
                consume(next)
                if (done) return
            }
        }
    }

    fun <T> exploreByTypes(collection: Collection<T>, block: (T) -> TypeExplorationResult?) {
        if (!done && collection.isNotEmpty()) {
            collection.asSequence().mapNotNull(block).forEach { next ->
                consume(next)
                if (done) return
            }
        }
    }

    private fun consume(exploredType: TypeExplorationResult) {
        when (exploredType) {
            is TypeExplorationResult.UsableType -> exploredType.classifierWithNarrowestVisibility?.accessibleClassifier?.let(::onFullyLinkedClassifier)
            is TypeExplorationResult.UnusableType.DueToClassifier -> onPartiallyLinkedClassifier(exploredType.classifier)
            is TypeExplorationResult.UnusableType.DueToVisibilityConflict -> onVisibilityConflict(exploredType)
        }
    }

    private fun consume(exploredClassifier: ExploredClassifier) {
        when (exploredClassifier) {
            is ExploredClassifier.Unusable -> onPartiallyLinkedClassifier(exploredClassifier)
            is ExploredClassifier.Usable -> onFullyLinkedClassifier(exploredClassifier.accessibleClassifier)
            is ExploredClassifier.RecursionAvoidance -> return // Just skip it.
        }
    }

    private inline val ExploredClassifier.Usable.accessibleClassifier: ExploredClassifier.Usable.AccessibleClassifier
        get() = when (this) {
            is ExploredClassifier.Usable.AccessibleClassifier -> this
            is ExploredClassifier.Usable.LesserAccessibleClassifier -> dueTo
        }

    protected fun checkDependencyVisibility(
        currentVisibility: ABIVisibility,
        nextClassifier: ExploredClassifier.Usable.AccessibleClassifier,
        onConflictingVisibilities: (ABIVisibility.Limited) -> UR
    ) {
        val nextVisibility = nextClassifier.visibility

        when (chooseNarrower(currentVisibility, nextVisibility)) {
            null -> unusableResult = onConflictingVisibilities(currentVisibility as ABIVisibility.Limited)
            currentVisibility -> Unit
            else -> dependencyWithNarrowerVisibility = nextClassifier
        }
    }

    protected abstract fun onPartiallyLinkedClassifier(exploredClassifier: ExploredClassifier.Unusable)
    protected abstract fun onFullyLinkedClassifier(exploredClassifier: ExploredClassifier.Usable.AccessibleClassifier)
    protected abstract fun onVisibilityConflict(exploredType: TypeExplorationResult.UnusableType.DueToVisibilityConflict)

    abstract fun build(): R
}

private class ExploredTypeBuilder : ExplorationResultBuilder<TypeExplorationResult, TypeExplorationResult.UnusableType>() {
    override fun onPartiallyLinkedClassifier(exploredClassifier: ExploredClassifier.Unusable) {
        unusableResult = TypeExplorationResult.UnusableType.DueToClassifier(exploredClassifier)
    }

    override fun onFullyLinkedClassifier(exploredClassifier: ExploredClassifier.Usable.AccessibleClassifier) {
        when (val dependencyWithNarrowerVisibility = dependencyWithNarrowerVisibility) {
            null -> {
                // Memoize the dependency if it has non-default public visibility.
                if (exploredClassifier.visibility != ABIVisibility.WholeWorld)
                    this.dependencyWithNarrowerVisibility = exploredClassifier
            }

            else -> {
                // Compare the visibility of the latest memoized dependency with the visibility of the `next` dependency.
                checkDependencyVisibility(dependencyWithNarrowerVisibility.visibility, exploredClassifier) {
                    TypeExplorationResult.UnusableType.DueToVisibilityConflict(dependencyWithNarrowerVisibility, exploredClassifier)
                }
            }
        }
    }

    override fun onVisibilityConflict(exploredType: TypeExplorationResult.UnusableType.DueToVisibilityConflict) {
        unusableResult = exploredType
    }

    override fun build(): TypeExplorationResult {
        return unusableResult
            ?: dependencyWithNarrowerVisibility?.let(TypeExplorationResult::UsableType)
            ?: TypeExplorationResult.UsableType.DEFAULT_PUBLIC
    }
}

private class ExploredClassifierBuilder(
    private val symbol: IrClassifierSymbol
) : (ExplorationResultBuilder<ExploredClassifier, ExploredClassifier>)() {
    private val ownVisibility = ABIVisibility.determineVisibilityFor(symbol.owner as IrDeclaration)

    override fun onPartiallyLinkedClassifier(exploredClassifier: ExploredClassifier.Unusable) {
        unusableResult = when (exploredClassifier) {
            is ExploredClassifier.Unusable.CanBeRootCause -> ExploredClassifier.Unusable.DueToOtherClassifier(symbol, exploredClassifier)
            is ExploredClassifier.Unusable.DueToOtherClassifier -> ExploredClassifier.Unusable.DueToOtherClassifier(symbol, exploredClassifier.rootCause)
        }
    }

    override fun onFullyLinkedClassifier(exploredClassifier: ExploredClassifier.Usable.AccessibleClassifier) {
        when (val dependencyWithNarrowerVisibility = dependencyWithNarrowerVisibility) {
            null -> {
                // Compare own visibility of the classifier with the visibility of the `next` dependency.
                checkDependencyVisibility(ownVisibility, exploredClassifier) { currentVisibility ->
                    ExploredClassifier.Unusable.InaccessibleClassifier(symbol, currentVisibility, exploredClassifier)
                }
            }

            else -> {
                // Compare the visibility of the latest memoized dependency with the visibility of the `next` dependency.
                checkDependencyVisibility(dependencyWithNarrowerVisibility.visibility, exploredClassifier) {
                    ExploredClassifier.Unusable.InaccessibleClassifierDueToOtherClassifiers(symbol, dependencyWithNarrowerVisibility, exploredClassifier)
                }
            }
        }
    }

    override fun onVisibilityConflict(exploredType: TypeExplorationResult.UnusableType.DueToVisibilityConflict) {
        unusableResult = ExploredClassifier.Unusable.InaccessibleClassifierDueToOtherClassifiers(
            symbol,
            exploredType.classifierWithConflictingVisibility1,
            exploredType.classifierWithConflictingVisibility2
        )
    }

    override fun build(): ExploredClassifier {
        return unusableResult
            ?: dependencyWithNarrowerVisibility?.let { ExploredClassifier.Usable.LesserAccessibleClassifier(symbol, it) }
            ?: ExploredClassifier.Usable.AccessibleClassifier(symbol, ownVisibility)
    }
}

private inline fun typeExploration(init: ExploredTypeBuilder.() -> Unit): TypeExplorationResult =
    ExploredTypeBuilder().apply(init).build()

private inline fun IrClassifierSymbol.classifierExploration(init: ExploredClassifierBuilder.() -> Unit): ExploredClassifier =
    ExploredClassifierBuilder(this).apply(init).build()

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
