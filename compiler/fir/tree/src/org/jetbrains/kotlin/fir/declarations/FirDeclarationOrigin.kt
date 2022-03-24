/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

sealed class FirDeclarationOrigin(private val displayName: String? = null, val fromSupertypes: Boolean = false, val generated: Boolean = false) {
    object Source : FirDeclarationOrigin()
    object Library : FirDeclarationOrigin()
    object Precompiled : FirDeclarationOrigin() // currently used for incremental compilation
    object BuiltIns : FirDeclarationOrigin()
    object Java : FirDeclarationOrigin()
    object Synthetic : FirDeclarationOrigin()
    object DynamicScope : FirDeclarationOrigin()
    object SamConstructor : FirDeclarationOrigin()
    object Enhancement : FirDeclarationOrigin()
    object ImportedFromObject : FirDeclarationOrigin()
    object SubstitutionOverride : FirDeclarationOrigin(fromSupertypes = true)
    object IntersectionOverride : FirDeclarationOrigin(fromSupertypes = true)
    object Delegated : FirDeclarationOrigin()
    object RenamedForOverride : FirDeclarationOrigin()
    object WrappedIntegerOperator : FirDeclarationOrigin()

    class Plugin(val key: FirPluginKey) : FirDeclarationOrigin(displayName = "Plugin[$key]", generated = true)

    override fun toString(): String {
        return displayName ?: this::class.simpleName!!
    }
}

abstract class FirPluginKey {
    val origin: FirDeclarationOrigin = FirDeclarationOrigin.Plugin(this)
}
