/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.dependencies

import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.asEntity
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.isNotPrivate
import org.jetbrains.kotlin.backend.common.dependencies.model.EnclosingEntity.Companion.parentEnclosingEntityOrSelf
import org.jetbrains.kotlin.backend.common.dependencies.util.hasCustomImplementation
import org.jetbrains.kotlin.backend.common.dependencies.util.isPrivate
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.symbols.IrAnonymousInitializerSymbol
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.FqName

sealed interface InitializationCycleAccessResult {
    val poisonsInitializers: Boolean get() = false

    sealed interface Reported : InitializationCycleAccessResult

    sealed class ReportedAndPoisoning : Reported {
        override val poisonsInitializers: Boolean = true
    }

    data class UninitializedPropertyAccess(val node: PropertyIndex) : ReportedAndPoisoning()

    data class UninitializedEnumEntryAccess(val node: EnumEntryIndex) : ReportedAndPoisoning()

    data class CyclicAccess(val node: DeclarationIndex<*>) : ReportedAndPoisoning()

    data class InaccessibleEntityAccess(val entity: EnclosingEntity<*>, val node: AccessibleIndex) : ReportedAndPoisoning()

    data class DeadlockInducingConstructorCall(val node: FunctionIndex.Constructor) : Reported

    data object PropagatesTransitiveDependencies : InitializationCycleAccessResult
}

sealed interface DependencyNodeIndex {
    val containingFile: IrFile? get() = null

    fun unwrap(): Set<DependencyNodeIndex> = setOf(this)

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

    context(accessingEntity: EnclosingEntity<*>?, cycle: CompositeNode?)
    val accessAnalysisResult: InitializationCycleAccessResult? get() = null
}

sealed interface DeclarationIndex<D : IrDeclaration> : DependencyNodeIndex {
    val symbol: IrBindableSymbol<*, D>

    override val containingFile: IrFile? get() = symbol.owner.fileOrNull
}

data class PropertyIndex(
    override val symbol: IrPropertySymbol,
    val enclosingEntity: EnclosingEntity<*>? = null,
) : DeclarationIndex<IrProperty>, AccessibleIndex {

    val isConst: Boolean get() = symbol.owner.isConst

    val hasInitializer: Boolean get() = symbol.owner.backingField?.initializer != null

    val hasFunctionType: Boolean = symbol.owner.getter?.returnType?.isFunctionOrKFunction()
        ?: symbol.owner.backingField?.type?.isFunctionOrKFunction()
        ?: false

    override val lazilyInitialized: EnclosingEntity<*>? = enclosingEntity?.parentEnclosingEntityOrSelf
        .takeIf { !isConst && !symbol.isPrivate }

    context(_: EnclosingEntity<*>?, _: CompositeNode?)
    override val accessAnalysisResult: InitializationCycleAccessResult?
        get() = when {
            !isConst && hasInitializer && !hasFunctionType -> InitializationCycleAccessResult.UninitializedPropertyAccess(this)
            else -> null
        }

    val getter: FunctionIndex.PropertyAccessor? = symbol.owner.getter?.takeIf { it.hasCustomImplementation }
        ?.let { FunctionIndex.PropertyAccessor(it.symbol, enclosingEntity) }

    val initializedClosure: FunctionIndex.Closure? = symbol.owner.backingField?.initializer?.expression?.let {
        when (it) {
            // We cover only simple cases e.g., `val x = { ... }`
            is IrFunctionExpression -> FunctionIndex.Closure(
                symbol = it.function.symbol,
                lazilyInitialized = lazilyInitialized
            )
            else -> null
        }
    }

    val name: FqName
        get() = symbol.owner.callableId.classId?.relativeClassName?.child(symbol.owner.name) ?: FqName.topLevel(symbol.owner.name)

    override fun toString(): String =
        "${symbol.owner.callableId.classId?.relativeClassName?.asString() ?: ""}.${symbol.owner.name.asString()}"
}

data class AnonymousInitializerIndex(
    override val symbol: IrAnonymousInitializerSymbol,
    val enclosingEntity: EnclosingEntity<*>? = null,
) : DeclarationIndex<IrAnonymousInitializer> {

    override fun toString(): String = "${symbol.owner.parentClassOrNull?.classId?.relativeClassName?.let { "$it." } ?: ""}<init_block>"
}

sealed class FunctionIndex<D : IrFunction> : DeclarationIndex<D>, AccessibleIndex {
    abstract override val symbol: IrBindableSymbol<*, D>

    context(enclosingEntity: EnclosingEntity<*>?, cycle: CompositeNode?)
    override val accessAnalysisResult: InitializationCycleAccessResult get() = InitializationCycleAccessResult.PropagatesTransitiveDependencies

    override fun toString(): String =
        "${symbol.owner.callableId.classId?.relativeClassName?.asString() ?: ""}.${symbol.owner.name.asString()}()"

    data class Closure(
        override val symbol: IrSimpleFunctionSymbol,
        override val lazilyInitialized: EnclosingEntity<*>? = null,
    ) : FunctionIndex<IrSimpleFunction>()

    data class Constructor(override val symbol: IrConstructorSymbol) : FunctionIndex<IrConstructor>() {

        override val lazilyInitialized: EnclosingEntity<*>? = symbol.owner.takeIf { it.isPrimary }
            ?.parentClassOrNull?.symbol?.asEntity(true)

        context(accessingEntity: EnclosingEntity<*>?, cycle: CompositeNode?)
        override val accessAnalysisResult: InitializationCycleAccessResult
            get() {
                return when {
                    lazilyInitialized?.let { accessingEntity?.parentEnclosingEntityOrSelf != it && /*it in cycle &&*/ it.isNotPrivate } == true -> {
                        InitializationCycleAccessResult.DeadlockInducingConstructorCall(this)
                    }
                    else -> InitializationCycleAccessResult.PropagatesTransitiveDependencies
                }
            }
    }

    data class MemberFunction(
        override val symbol: IrSimpleFunctionSymbol,
        override val lazilyInitialized: EnclosingEntity<*>? = null,
    ) : FunctionIndex<IrSimpleFunction>()

    data class PropertyAccessor(
        override val symbol: IrSimpleFunctionSymbol,
        override val lazilyInitialized: EnclosingEntity<*>? = null,
    ) : FunctionIndex<IrSimpleFunction>()
}

data class DefaultedFunctionIndex<D : IrFunction>(
    val functionIndex: FunctionIndex<D>,
    val defaultParameters: Set<IrValueParameterSymbol>
) : FunctionIndex<D>() {
    override val symbol: IrBindableSymbol<*, D> get() = functionIndex.symbol

    // It is redundant to create another edge from the lazily initialized entity of the original function node to this node,
    // the cycle is already subsumed by the original function node
    override val lazilyInitialized: EnclosingEntity<*>? = null

    context(_: EnclosingEntity<*>?, _: CompositeNode?)
    override val accessAnalysisResult: InitializationCycleAccessResult get() = functionIndex.accessAnalysisResult
}

sealed class BeginStaticInitializationIndex<D : IrSymbolOwner> : DependencyNodeIndex {
    abstract val enclosingEntity: EnclosingEntity<D>

    override val containingFile: IrFile? get() = enclosingEntity.containingFile

    override fun toString(): String = "Begin $enclosingEntity"
}

data class EndStaticInitializationIndex<D : IrSymbolOwner>(val enclosingEntity: EnclosingEntity<D>) : DependencyNodeIndex {

    override val containingFile: IrFile? get() = enclosingEntity.containingFile

    override fun toString(): String = "End $enclosingEntity"
}

data class TopLevelIndex(override val enclosingEntity: EnclosingEntity.File) : BeginStaticInitializationIndex<IrFile>() {
    override fun toString(): String = "<$enclosingEntity>"
}

data class QualifierIndex(
    override val enclosingEntity: EnclosingEntity.Object
) : BeginStaticInitializationIndex<IrClass>(), AccessibleIndex {

    override val lazilyInitialized: EnclosingEntity<*>? = enclosingEntity.takeIf { it.isNotPrivate }?.parentEnclosingEntityOrSelf

    context(_: EnclosingEntity<*>?, _: CompositeNode?)
    override val accessAnalysisResult: InitializationCycleAccessResult get() = InitializationCycleAccessResult.PropagatesTransitiveDependencies
}

data class EnumEntryIndex(
    override val enclosingEntity: EnclosingEntity.EnumEntry
) : BeginStaticInitializationIndex<IrEnumEntry>(), AccessibleIndex {

    override val lazilyInitialized: EnclosingEntity<*>? = enclosingEntity.parentEnclosingEntity.takeIf { it.isNotPrivate }

    context(_: EnclosingEntity<*>?, _: CompositeNode?)
    override val accessAnalysisResult: InitializationCycleAccessResult
        get() = InitializationCycleAccessResult.UninitializedEnumEntryAccess(this)
}

data class ClinitIndex(override val enclosingEntity: EnclosingEntity.Class) : BeginStaticInitializationIndex<IrClass>() {
    override fun toString(): String = "${super.toString()}.<clinit>"
}

data class BeginInstanceInitializationIndex(val symbol: IrClassSymbol) : DependencyNodeIndex {

    override fun toString(): String = "Begin ${symbol.owner.let { it.classId?.relativeClassName ?: FqName.topLevel(it.name) }}.<init>"
}

data class EndInstanceInitializationIndex(val symbol: IrClassSymbol) : DependencyNodeIndex {

    override fun toString(): String = "End ${symbol.owner.let { it.classId?.relativeClassName ?: FqName.topLevel(it.name) }}.<init>"
}

data class CompositeIndex(val indices: Set<DependencyNodeIndex>) : DependencyNodeIndex {
    override fun unwrap(): Set<DependencyNodeIndex> = indices
    override fun toString(): String = indices.joinToString(prefix = "{", postfix = "}")
}
