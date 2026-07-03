/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirClass
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
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName

sealed interface InitializationCycleAccessResult {
    val poisonsInitializers: Boolean

    data class UninitializedPropertyAccess(val node: PropertyIndex) : InitializationCycleAccessResult {
        override val poisonsInitializers: Boolean = true
    }

    data class UninitializedEnumEntryAccess(val node: EnumEntryIndex) : InitializationCycleAccessResult {
        override val poisonsInitializers: Boolean = true
    }

    data class CyclicAccess(val node: DeclarationIndex<*>) : InitializationCycleAccessResult {
        override val poisonsInitializers: Boolean = true
    }

    data class InaccessibleEntityAccess(val entity: EnclosingEntity<*>, val node: AccessibleIndex) : InitializationCycleAccessResult {
        override val poisonsInitializers: Boolean = true
    }

    data class DeadlockInducingConstructorCall(val node: FunctionIndex.Constructor) : InitializationCycleAccessResult {
        override val poisonsInitializers: Boolean = false
    }

    data object PropagatesTransitiveDependencies : InitializationCycleAccessResult {
        override val poisonsInitializers: Boolean = false
    }

    data object Safe : InitializationCycleAccessResult {
        override val poisonsInitializers: Boolean = false
    }
}

sealed interface DependencyNodeIndex {
    context(_: SessionHolder)
    val containingFile: FirFileSymbol? get() = null

    companion object {
        val DependencyNodeIndex.enclosingEntity: EnclosingEntity<*>?
            get() = when (this) {
                is BeginStaticInitializationIndex<*> -> enclosingEntity
                is EndStaticInitializationIndex<*> -> enclosingEntity
                is PropertyIndex -> enclosingEntity
                is AnonymousInitializerIndex -> enclosingEntity
                is FunctionIndex<*> -> lazilyInitialized
                else -> null
            }
    }
}

sealed interface AccessibleIndex : DependencyNodeIndex {
    val lazilyInitialized: EnclosingEntity<*>?

    context(accessingEntity: EnclosingEntity<*>?, cycle: CompositeNode)
    val accessAnalysisResult: InitializationCycleAccessResult get() = InitializationCycleAccessResult.Safe
}

sealed interface DeclarationIndex<D : FirDeclaration> : DependencyNodeIndex {
    val symbol: FirBasedSymbol<D>
}

data class PropertyIndex(
    override val symbol: FirPropertySymbol,
    val enclosingEntity: EnclosingEntity<*>? = null,
) : DeclarationIndex<FirProperty>, AccessibleIndex {

    val isConst: Boolean get() = symbol.isConst

    val hasInitializer: Boolean get() = symbol.hasInitializer

    val hasFunctionType: Boolean = symbol.resolvedReturnType.isSomeFunctionType(symbol.moduleData.session)

    override val lazilyInitialized: EnclosingEntity<*>? = enclosingEntity?.parentEnclosingEntityOrSelf
        .takeIf { !isConst && !symbol.isPrivate }

    context(_: EnclosingEntity<*>?, _: CompositeNode)
    override val accessAnalysisResult: InitializationCycleAccessResult
        get() = when {
            !isConst && hasInitializer && !hasFunctionType -> InitializationCycleAccessResult.UninitializedPropertyAccess(this)
            else -> InitializationCycleAccessResult.Safe
        }

    val getter: FunctionIndex.PropertyAccessor? = symbol.getterSymbol
        ?.takeIf { it.hasCustomImplementation }
        ?.let { FunctionIndex.PropertyAccessor(it, enclosingEntity) }

    val initializedClosure: FunctionIndex.Closure? = symbol.fir.initializer?.let {
        when (it) {
            // We cover only simple cases e.g., `val x = { ... }`
            is FirAnonymousFunctionExpression -> FunctionIndex.Closure(
                symbol = it.anonymousFunction.symbol,
                lazilyInitialized = lazilyInitialized
            )
            else -> null
        }
    }

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = sessionHolder.session.firProvider.getContainingFile(symbol)?.symbol

    val name: FqName get() = symbol.callableId?.classId?.relativeClassName?.child(symbol.name) ?: CallableId(symbol.name).asSingleFqName()

    override fun toString(): String = "${symbol.callableId?.classId?.relativeClassName ?: ""}.${symbol.name.asString()}"
}

data class AnonymousInitializerIndex(
    override val symbol: FirAnonymousInitializerSymbol,
    val enclosingEntity: EnclosingEntity<*>? = null,
) : DeclarationIndex<FirAnonymousInitializer> {

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = sessionHolder.session.firProvider.getContainingFile(symbol.containingDeclarationSymbol)?.symbol

    override fun toString(): String =
        "${(symbol.containingDeclarationSymbol as? FirClassSymbol<*>)?.classId?.relativeClassName?.asString() ?: ""}.<init-block>"
}

sealed class FunctionIndex<D : FirFunction> : DeclarationIndex<FirFunction>, AccessibleIndex {
    abstract override val symbol: FirFunctionSymbol<D>

    context(_: EnclosingEntity<*>?, _: CompositeNode)
    override val accessAnalysisResult: InitializationCycleAccessResult get() = InitializationCycleAccessResult.PropagatesTransitiveDependencies

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = sessionHolder.session.firProvider.getContainingFile(symbol)?.symbol

    override fun toString(): String = "${symbol.callableId.classId?.relativeClassName?.asString() ?: ""}.${symbol.name.asString()}()"

    data class Closure(
        override val symbol: FirAnonymousFunctionSymbol,
        override val lazilyInitialized: EnclosingEntity<*>? = null,
    ) : FunctionIndex<FirAnonymousFunction>()

    data class Constructor(override val symbol: FirConstructorSymbol) : FunctionIndex<FirConstructor>() {

        override val lazilyInitialized: EnclosingEntity<*>? =
            symbol.takeIf { it.isPrimary }
                ?.getContainingClassSymbol()
                ?.fullyExpandedClass(symbol.moduleData.session)
                ?.asEntity(symbol.moduleData.session, true)

        context(accessingEntity: EnclosingEntity<*>?, cycle: CompositeNode)
        override val accessAnalysisResult: InitializationCycleAccessResult
            get() {
                return when {
                    lazilyInitialized?.let { accessingEntity?.parentEnclosingEntityOrSelf != it && it in cycle } == true -> {
                        InitializationCycleAccessResult.DeadlockInducingConstructorCall(this)
                    }
                    else -> InitializationCycleAccessResult.PropagatesTransitiveDependencies
                }
            }
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

    // It is redundant to create another edge from the lazily initialized entity of the original function node to this node as well
    override val lazilyInitialized: EnclosingEntity<*>? get() = null

    context(_: EnclosingEntity<*>?, _: CompositeNode)
    override val accessAnalysisResult: InitializationCycleAccessResult get() = functionIndex.accessAnalysisResult
}

sealed interface BeginStaticInitializationIndex<D : FirDeclaration> : DependencyNodeIndex {
    val enclosingEntity: EnclosingEntity<D>

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = sessionHolder.session.firProvider.getContainingFile(enclosingEntity.symbol)?.symbol
}

data class EndStaticInitializationIndex<D : FirDeclaration>(val enclosingEntity: EnclosingEntity<D>) : DependencyNodeIndex {

    context(sessionHolder: SessionHolder)
    override val containingFile: FirFileSymbol? get() = sessionHolder.session.firProvider.getContainingFile(enclosingEntity.symbol)?.symbol
}

data class TopLevelIndex(
    override val enclosingEntity: EnclosingEntity.File
) : BeginStaticInitializationIndex<FirFile> {
    override fun toString(): String = "<$enclosingEntity>"
}

data class QualifierIndex(
    override val enclosingEntity: EnclosingEntity.Object
) : BeginStaticInitializationIndex<FirRegularClass>, AccessibleIndex {

    override val lazilyInitialized: EnclosingEntity<*>? =
        enclosingEntity.takeIf { it.isNotPrivate }?.let { it.parentEnclosingEntity ?: it }

    context(_: EnclosingEntity<*>?, _: CompositeNode)
    override val accessAnalysisResult: InitializationCycleAccessResult get() = InitializationCycleAccessResult.PropagatesTransitiveDependencies

    override fun toString(): String = enclosingEntity.toString()
}

data class EnumEntryIndex(
    override val enclosingEntity: EnclosingEntity.EnumEntry
) : BeginStaticInitializationIndex<FirEnumEntry>, AccessibleIndex {

    override val lazilyInitialized: EnclosingEntity<*>? = enclosingEntity.parentEnclosingEntity.takeIf { it.isNotPrivate }

    context(_: EnclosingEntity<*>?, _: CompositeNode)
    override val accessAnalysisResult: InitializationCycleAccessResult
        get() = InitializationCycleAccessResult.UninitializedEnumEntryAccess(this)

    override fun toString(): String = enclosingEntity.toString()
}

data class ClinitIndex(override val enclosingEntity: EnclosingEntity.Class) : BeginStaticInitializationIndex<FirRegularClass> {
    override fun toString(): String = "$enclosingEntity.<clinit>"
}

data class BeginInstanceInitializationIndex<C : FirClass>(val classSymbol: FirClassSymbol<C>) : DependencyNodeIndex {

    override fun toString(): String = "Begin ${classSymbol.classId.asString()}.<init>"
}

data class EndInstanceInitializationIndex<C : FirClass>(val classSymbol: FirClassSymbol<C>) : DependencyNodeIndex {

    override fun toString(): String = "End ${classSymbol.classId.asString()}.<init>"
}

data class CompositeIndex(val indices: Set<DependencyNodeIndex>) : DependencyNodeIndex {
    override fun toString(): String = indices.joinToString(prefix = "{", postfix = "}")
}
