/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.declarations.utils.FirScriptCustomizationKind

sealed class FirDeclarationOrigin(
    private val displayName: String? = null,
    val fromSupertypes: Boolean = false,
    val generated: Boolean = false,
    val fromSource: Boolean = false,
    val generatedAnyMethod: Boolean = false,
) {
    object Source : FirDeclarationOrigin(fromSource = true)
    object Library : FirDeclarationOrigin()
    object Precompiled : FirDeclarationOrigin() // currently used for incremental compilation
    object BuiltIns : FirDeclarationOrigin()
    sealed class Java(displayName: String, fromSource: Boolean = false) : FirDeclarationOrigin(displayName, fromSource = fromSource) {
        object Source : Java("Java(Source)", fromSource = true)
        object Library : Java("Java(Library)")
    }

    sealed class Synthetic(generatedAnyMethod: Boolean = false) : FirDeclarationOrigin(generatedAnyMethod = generatedAnyMethod) {
        object DataClassMember : Synthetic(generatedAnyMethod = true)
        object ValueClassMember : Synthetic(generatedAnyMethod = true)
        object JavaProperty : Synthetic()
        object DelegateField : Synthetic()
        object PluginFile : Synthetic()
        object Error : Synthetic()
        object TypeAliasConstructor : Synthetic()
        object FakeFunction : Synthetic()
        object ForwardDeclaration : Synthetic()
        object ScriptTopLevelDestructuringDeclarationContainer : Synthetic()
        object FakeHiddenInPreparationForNewJdk : Synthetic()
    }
    object DynamicScope : FirDeclarationOrigin()
    object SamConstructor : FirDeclarationOrigin()
    object Enhancement : FirDeclarationOrigin()
    object ImportedFromObjectOrStatic : FirDeclarationOrigin()
    sealed class SubstitutionOverride(displayName: String) : FirDeclarationOrigin(displayName, fromSupertypes = true) {
        object DeclarationSite : SubstitutionOverride("SubstitutionOverride(DeclarationSite)")
        object CallSite : SubstitutionOverride("SubstitutionOverride(CallSite)")
    }

    object IntersectionOverride : FirDeclarationOrigin(fromSupertypes = true)
    object Delegated : FirDeclarationOrigin()
    object RenamedForOverride : FirDeclarationOrigin()
    object WrappedIntegerOperator : FirDeclarationOrigin()
    sealed class ScriptCustomization(val kind: FirScriptCustomizationKind) : FirDeclarationOrigin() {
        object Default : ScriptCustomization(FirScriptCustomizationKind.DEFAULT)
        object ResultProperty : ScriptCustomization(FirScriptCustomizationKind.RESULT_PROPERTY)
        object Parameter : ScriptCustomization(FirScriptCustomizationKind.PARAMETER)
    }
    class Plugin(val key: GeneratedDeclarationKey) : FirDeclarationOrigin(displayName = "Plugin[$key]", generated = true)

    override fun toString(): String {
        return displayName ?: this::class.simpleName!!
    }
}

val GeneratedDeclarationKey.origin: FirDeclarationOrigin
    get() = FirDeclarationOrigin.Plugin(this)

/**
 * @return **true** if a declaration with [this] origin can be in not fully resolved state
 */
val FirDeclarationOrigin.isLazyResolvable: Boolean
    get() = when (this) {
        is FirDeclarationOrigin.Source,
        is FirDeclarationOrigin.ImportedFromObjectOrStatic,
        is FirDeclarationOrigin.Delegated,
        is FirDeclarationOrigin.Synthetic,
        is FirDeclarationOrigin.SubstitutionOverride,
        is FirDeclarationOrigin.SamConstructor,
        is FirDeclarationOrigin.WrappedIntegerOperator,
        is FirDeclarationOrigin.IntersectionOverride,
        is FirDeclarationOrigin.ScriptCustomization,
        -> true
        else -> false
    }