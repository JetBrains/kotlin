/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.GeneratedDeclarationKey

sealed class FirDeclarationOrigin(
    private val displayName: String? = null,
    val fromSupertypes: Boolean = false,
    val generated: Boolean = false,
    val fromSource: Boolean = false
) {
    object Source : FirDeclarationOrigin(fromSource = true)
    object Library : FirDeclarationOrigin()
    object Precompiled : FirDeclarationOrigin() // currently used for incremental compilation
    object BuiltIns : FirDeclarationOrigin()
    sealed class Java(displayName: String, fromSource: Boolean = false) : FirDeclarationOrigin(displayName, fromSource = fromSource) {
        object Source : Java("Java(Source)", fromSource = true)
        object Library : Java("Java(Library)")
    }

    object Synthetic : FirDeclarationOrigin()
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
    object ScriptCustomization : FirDeclarationOrigin()
    class Plugin(val key: GeneratedDeclarationKey) : FirDeclarationOrigin(displayName = "Plugin[$key]", generated = true)

    override fun toString(): String {
        return displayName ?: this::class.simpleName!!
    }
}

val GeneratedDeclarationKey.origin: FirDeclarationOrigin
    get() = FirDeclarationOrigin.Plugin(this)
