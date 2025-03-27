/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name

private object IsFromVarargKey : FirDeclarationDataKey()
private object IsReferredViaField : FirDeclarationDataKey()
private object IsFromPrimaryConstructor : FirDeclarationDataKey()
private object ComponentFunctionSymbolKey : FirDeclarationDataKey()
private object SourceElementKey : FirDeclarationDataKey()
private object ModuleNameKey : FirDeclarationDataKey()
private object DanglingTypeConstraintsKey : FirDeclarationDataKey()
private object KlibSourceFile : FirDeclarationDataKey()
private object EvaluatedValue : FirDeclarationDataKey()
private object CompilerPluginMetadata : FirDeclarationDataKey()
private object OriginalReplSnippet : FirDeclarationDataKey()
private object ReplSnippetTopLevelDeclaration : FirDeclarationDataKey()
private object HasBackingFieldKey : FirDeclarationDataKey()
private object IsDeserializedPropertyFromAnnotation : FirDeclarationDataKey()
private object IsDelegatedProperty : FirDeclarationDataKey()
private object LambdaArgumentHoldsInTruths : FirDeclarationDataKey()

var FirProperty.isFromVararg: Boolean? by FirDeclarationDataRegistry.data(IsFromVarargKey)
var FirProperty.isReferredViaField: Boolean? by FirDeclarationDataRegistry.data(IsReferredViaField)
var FirProperty.fromPrimaryConstructor: Boolean? by FirDeclarationDataRegistry.data(IsFromPrimaryConstructor)
var FirProperty.componentFunctionSymbol: FirNamedFunctionSymbol? by FirDeclarationDataRegistry.data(ComponentFunctionSymbolKey)
var FirClassLikeDeclaration.sourceElement: SourceElement? by FirDeclarationDataRegistry.data(SourceElementKey)
var FirRegularClass.moduleName: String? by FirDeclarationDataRegistry.data(ModuleNameKey)
var FirDeclaration.compilerPluginMetadata: Map<String, ByteArray>? by FirDeclarationDataRegistry.data(CompilerPluginMetadata)
var FirDeclaration.originalReplSnippetSymbol: FirReplSnippetSymbol? by FirDeclarationDataRegistry.data(OriginalReplSnippet)
var FirAnonymousFunction.lambdaArgumentHoldsInTruths: List<FirExpression>? by FirDeclarationDataRegistry.data(LambdaArgumentHoldsInTruths)

/**
 * Denotes a declaration on the REPL snippet level - top-level and all nested ones, but not the ones declared inside bodies.
 * Required to distinguish these declarations from "real" local ones, declared in bodies.
 * TODO: Revisit along with KT-75301
 */
var FirDeclaration.isReplSnippetDeclaration: Boolean? by FirDeclarationDataRegistry.data(ReplSnippetTopLevelDeclaration)
val FirBasedSymbol<*>.isReplSnippetDeclaration: Boolean?
    get() = fir.isReplSnippetDeclaration

/**
 * This is an implementation detail attribute to provide proper [hasBackingField]
 * flag for deserialized properties.
 *
 * This attribute mustn't be used directly.
 *
 * @see hasBackingField
 */
@FirImplementationDetail
var FirProperty.hasBackingFieldAttr: Boolean? by FirDeclarationDataRegistry.data(HasBackingFieldKey)


/**
 * This is an implementation detail attribute to provide proper [isDelegatedProperty]
 * flag for deserialized properties.
 *
 * This attribute mustn't be used directly.
 *
 * It is either *true* or *null*, because *false* is a default value for deserialized properties.
 *
 * @see isDelegatedProperty
 */
@FirImplementationDetail
var FirProperty.isDelegatedPropertyAttr: Boolean? by FirDeclarationDataRegistry.data(IsDelegatedProperty)

/**
 * Whether this property was deserialized from metadata and the containing class is annotation class.
 */
var FirProperty.isDeserializedPropertyFromAnnotation: Boolean? by FirDeclarationDataRegistry.data(IsDeserializedPropertyFromAnnotation)

/**
 * @see [FirBasedSymbol.klibSourceFile]
 */
var FirDeclaration.klibSourceFile: SourceFile? by FirDeclarationDataRegistry.data(KlibSourceFile)

val FirClassLikeSymbol<*>.sourceElement: SourceElement?
    get() = fir.sourceElement

val FirPropertySymbol.fromPrimaryConstructor: Boolean
    get() = fir.fromPrimaryConstructor ?: false

/**
 * Declarations like classes, functions, and properties can encode their containing Kotlin source file into .klibs using
 * klib specific metadata extensions.
 * If present in the klib and deserialized by the corresponding deserializer/symbol provider,
 * then this source file is available here
 * @see FirDeclaration.klibSourceFile
 */
val FirBasedSymbol<FirDeclaration>.klibSourceFile: SourceFile?
    get() = fir.klibSourceFile

var FirProperty.evaluatedInitializer: FirEvaluatorResult? by FirDeclarationDataRegistry.data(EvaluatedValue)

/**
 * Constraint without corresponding type argument
 */
data class DanglingTypeConstraint(val name: Name, val source: KtSourceElement)

var <T> T.danglingTypeConstraints: List<DanglingTypeConstraint>?
        where T : FirDeclaration, T : FirTypeParameterRefsOwner
        by FirDeclarationDataRegistry.data(DanglingTypeConstraintsKey)

// ----------------------------------- Utils -----------------------------------

val FirProperty.hasExplicitBackingField: Boolean
    get() = backingField != null && backingField !is FirDefaultPropertyBackingField

val FirPropertySymbol.hasExplicitBackingField: Boolean
    get() = fir.hasExplicitBackingField

fun FirProperty.getExplicitBackingField(): FirBackingField? {
    return if (hasExplicitBackingField) {
        backingField
    } else {
        null
    }
}

val FirProperty.canNarrowDownGetterType: Boolean
    get() {
        val backingFieldHasDifferentType = backingField != null && backingField?.returnTypeRef?.coneType != returnTypeRef.coneType
        return backingFieldHasDifferentType && getter is FirDefaultPropertyGetter
    }

val FirPropertySymbol.canNarrowDownGetterType: Boolean
    get() = fir.canNarrowDownGetterType

val FirProperty.isDelegatedProperty: Boolean
    @OptIn(FirImplementationDetail::class)
    get() = isDelegatedPropertyAttr ?: (delegate != null)

val FirPropertySymbol.isDelegatedProperty: Boolean
    get() = fir.isDelegatedProperty

// See [BindingContext.BACKING_FIELD_REQUIRED]
val FirProperty.hasBackingField: Boolean
    get() {
        @OptIn(FirImplementationDetail::class)
        hasBackingFieldAttr?.let { return it }

        if (isAbstract || isExpect) return false
        if (delegate != null) return false
        if (hasExplicitBackingField) return true
        if (symbol is FirSyntheticPropertySymbol) return false
        if (isStatic) return false // For Enum.entries
        when (origin) {
            is FirDeclarationOrigin.SubstitutionOverride -> return false
            FirDeclarationOrigin.IntersectionOverride -> return false
            FirDeclarationOrigin.Delegated -> return false
            else -> {
                val getter = getter ?: return true
                if (isVar && setter == null) return true
                if (setter?.hasBody == false && setter?.isAbstract == false) return true
                if (!getter.hasBody && !getter.isAbstract) return true

                return isReferredViaField == true
            }
        }
    }

val FirPropertySymbol.hasBackingField: Boolean
    get() {
        lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return fir.hasBackingField
    }

fun FirDeclaration.getDanglingTypeConstraintsOrEmpty(): List<DanglingTypeConstraint> {
    return when (this) {
        is FirRegularClass -> danglingTypeConstraints
        is FirSimpleFunction -> danglingTypeConstraints
        is FirAnonymousFunction -> danglingTypeConstraints
        is FirProperty -> danglingTypeConstraints
        else -> null
    } ?: emptyList()
}

val FirPropertySymbol.correspondingValueParameterFromPrimaryConstructor: FirValueParameterSymbol?
    get() = fir.correspondingValueParameterFromPrimaryConstructor

val FirProperty.correspondingValueParameterFromPrimaryConstructor: FirValueParameterSymbol?
    get() {
        if (fromPrimaryConstructor != true) return null
        val initializer = initializer as? FirPropertyAccessExpression ?: return null
        val reference = initializer.calleeReference as? FirPropertyFromParameterResolvedNamedReference ?: return null
        return reference.resolvedSymbol as? FirValueParameterSymbol
    }
