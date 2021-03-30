/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

sealed class FirDeclarationOrigin(private val displayName: String? = null, val fromSupertypes: Boolean = false) {
    object Source : FirDeclarationOrigin()
    object Library : FirDeclarationOrigin()
    object BuiltIns : FirDeclarationOrigin()
    object Java : FirDeclarationOrigin()
    object Synthetic : FirDeclarationOrigin()
    object SamConstructor : FirDeclarationOrigin()
    object Enhancement : FirDeclarationOrigin()
    object ImportedFromObject : FirDeclarationOrigin()
    object SubstitutionOverride : FirDeclarationOrigin(fromSupertypes = true)
    object IntersectionOverride : FirDeclarationOrigin(fromSupertypes = true)
    object Delegated : FirDeclarationOrigin()

    class Plugin(val key: FirPluginKey) : FirDeclarationOrigin(displayName = "Plugin[$key]")

    override fun toString(): String {
        return displayName ?: this::class.simpleName!!
    }
}

abstract class FirPluginKey
