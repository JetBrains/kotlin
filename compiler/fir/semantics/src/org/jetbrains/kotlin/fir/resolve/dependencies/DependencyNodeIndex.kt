/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.isNotPrivate
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.parentEnclosingEntityOrSelf
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.isSomeFunctionType

enum class CyclicAccessTraversalAction {
    POSSIBLY_UNINITIALIZED,
    TRANSITIVELY_CONTINUE,
    IGNORE
}

sealed interface DependencyNodeIndex {
    context(_: SessionHolder)
    val containingFile: FirFileSymbol? get() = null
}

sealed interface AccessibleIndex : DependencyNodeIndex {
    val lazilyInitialized: EnclosingEntity<*>?
    val traversalAction: CyclicAccessTraversalAction get() = CyclicAccessTraversalAction.IGNORE
}

sealed interface StaticInitializationIndex : DependencyNodeIndex {
    val enclosingEntity: EnclosingEntity<*>
}

sealed interface DeclarationIndex<D : FirDeclaration> : DependencyNodeIndex {
    val symbol: FirBasedSymbol<D>
}

data class StaticPropertyIndex(
    override val enclosingEntity: EnclosingEntity<*>,
    override val symbol: FirPropertySymbol
) : DeclarationIndex<FirProperty>, StaticInitializationIndex, AccessibleIndex {

    val isConst: Boolean get() = symbol.isConst

    val hasInitializer: Boolean get() = symbol.hasInitializer

    val hasFunctionType: Boolean = symbol.resolvedReturnType.isSomeFunctionType(symbol.moduleData.session)

    override val lazilyInitialized: EnclosingEntity<*> = enclosingEntity.parentEnclosingEntityOrSelf

    override val traversalAction: CyclicAccessTraversalAction = when {
        !isConst && hasInitializer && !hasFunctionType -> CyclicAccessTraversalAction.POSSIBLY_UNINITIALIZED
        else -> CyclicAccessTraversalAction.IGNORE
    }

    val getter: FunctionIndex.PropertyAccessor? = symbol.getterSymbol
        ?.takeIf { it.hasCustomImplementation }
        ?.let { FunctionIndex.PropertyAccessor(it, enclosingEntity) }

    val initializedClosure: FunctionIndex.Closure? = symbol.fir.initializer?.let {
        when (it) {
            // We cover only simple cases e.g., `val x = { ... }`
            is FirAnonymousFunctionExpression -> FunctionIndex.Closure(it.anonymousFunction.symbol, lazilyInitialized)
            else -> null
        }
    }

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = sessionHolder.session.firProvider.getContainingFile(symbol)?.symbol

    override fun toString(): String = "${symbol.callableId?.classId?.relativeClassName ?: ""}.${symbol.name.asString()}"
}

data class StaticAnonymousInitializerIndex(
    override val enclosingEntity: EnclosingEntity<*>,
    override val symbol: FirAnonymousInitializerSymbol
) : DeclarationIndex<FirAnonymousInitializer>, StaticInitializationIndex {

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = sessionHolder.session.firProvider.getContainingFile(symbol.containingDeclarationSymbol)?.symbol

    override fun toString(): String =
        "${(symbol.containingDeclarationSymbol as? FirClassSymbol<*>)?.classId?.relativeClassName?.asString() ?: ""}.<init-block>"
}

sealed class FunctionIndex<D : FirFunction> : DeclarationIndex<FirFunction>, AccessibleIndex {
    abstract override val symbol: FirFunctionSymbol<D>

    override val traversalAction: CyclicAccessTraversalAction = CyclicAccessTraversalAction.TRANSITIVELY_CONTINUE

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = sessionHolder.session.firProvider.getContainingFile(symbol)?.symbol

    override fun toString(): String = "${symbol.callableId.classId?.relativeClassName?.asString() ?: ""}.${symbol.name.asString()}()"

    data class Closure(
        override val symbol: FirAnonymousFunctionSymbol,
        override val lazilyInitialized: EnclosingEntity<*>? = null,
    ) : FunctionIndex<FirAnonymousFunction>()

    data class Constructor(override val symbol: FirConstructorSymbol) : FunctionIndex<FirConstructor>() {

        override val lazilyInitialized: EnclosingEntity<*>? =
            symbol.takeIf { it.isPrimary }?.getContainingClassSymbol()
                ?.fullyExpandedClass(symbol.moduleData.session)
                ?.asEntity(symbol.moduleData.session, true)
    }

    data class MemberFunction(
        override val symbol: FirNamedFunctionSymbol,
        override val lazilyInitialized: EnclosingEntity<*>? = null,
    ) : FunctionIndex<FirNamedFunction>()

    data class PropertyAccessor(
        override val symbol: FirPropertyAccessorSymbol,
        override val lazilyInitialized: EnclosingEntity<*>? = null,
    ) : FunctionIndex<FirPropertyAccessor>()
}

data class DefaultedFunctionIndex<D : FirFunction>(
    val functionIndex: FunctionIndex<D>,
    val defaultParameters: Set<FirValueParameterSymbol>
) : FunctionIndex<D>() {
    override val symbol: FirFunctionSymbol<D> get() = functionIndex.symbol

    override val lazilyInitialized: EnclosingEntity<*>? get() = null

    override val traversalAction: CyclicAccessTraversalAction get() = functionIndex.traversalAction
}

sealed interface BeginInitializationIndex<D : FirDeclaration> : StaticInitializationIndex {
    override val enclosingEntity: EnclosingEntity<D>

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = sessionHolder.session.firProvider.getContainingFile(enclosingEntity.symbol)?.symbol

}

data class EndInitializationIndex<D : FirDeclaration>(val beginIndex: BeginInitializationIndex<D>) : StaticInitializationIndex {
    override val enclosingEntity: EnclosingEntity<*> get() = beginIndex.enclosingEntity

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = beginIndex.containingFile
}

data class TopLevelIndex(
    override val enclosingEntity: EnclosingEntity.File
) : BeginInitializationIndex<FirFile> {
    override fun toString(): String = "<$enclosingEntity>"
}

data class QualifierIndex(
    override val enclosingEntity: EnclosingEntity.Object
) : BeginInitializationIndex<FirRegularClass>, AccessibleIndex {

    override val lazilyInitialized: EnclosingEntity<*>? =
        enclosingEntity.takeIf { it.isNotPrivate }?.let { it.parentEnclosingEntity ?: it }

    override val traversalAction: CyclicAccessTraversalAction = CyclicAccessTraversalAction.TRANSITIVELY_CONTINUE

    override fun toString(): String = enclosingEntity.toString()
}

data class EnumEntryIndex(
    override val enclosingEntity: EnclosingEntity.EnumEntry
) : BeginInitializationIndex<FirEnumEntry>, AccessibleIndex {

    override val lazilyInitialized: EnclosingEntity<*>? = enclosingEntity.parentEnclosingEntity.takeIf { it.isNotPrivate }

    override val traversalAction: CyclicAccessTraversalAction = CyclicAccessTraversalAction.POSSIBLY_UNINITIALIZED

    override fun toString(): String = enclosingEntity.toString()
}

data class ClinitIndex(override val enclosingEntity: EnclosingEntity.Class) : BeginInitializationIndex<FirRegularClass> {
    override fun toString(): String = "$enclosingEntity.<clinit>"
}

data class CompositeIndex(val indices: Set<DependencyNodeIndex>) : DependencyNodeIndex {
    override fun toString(): String = indices.joinToString(prefix = "{", postfix = "}")
}
