/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSyntheticJavaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.Name

@KaImplementationDetail
fun ClassKind.toKtClassKind(isCompanionObject: Boolean): KaClassKind = when (this) {
    ClassKind.INTERFACE -> KaClassKind.INTERFACE
    ClassKind.ENUM_CLASS -> KaClassKind.ENUM_CLASS
    ClassKind.ANNOTATION_CLASS -> KaClassKind.ANNOTATION_CLASS
    ClassKind.CLASS -> KaClassKind.CLASS
    ClassKind.OBJECT -> if (isCompanionObject) KaClassKind.COMPANION_OBJECT else KaClassKind.OBJECT
    ClassKind.ENUM_ENTRY -> invalidEnumEntryAsClassKind()
}

@KaImplementationDetail
fun invalidEnumEntryAsClassKind(): Nothing {
    error("KtClassKind is not applicable for enum entry, as enum entry is a callable, not a classifier")
}

@KaImplementationDetail
val Modality.asKaSymbolModality: KaSymbolModality
    get() = when (this) {
        Modality.FINAL -> KaSymbolModality.FINAL
        Modality.SEALED -> KaSymbolModality.SEALED
        Modality.OPEN -> KaSymbolModality.OPEN
        Modality.ABSTRACT -> KaSymbolModality.ABSTRACT
    }

@KaImplementationDetail
enum class KaSyntheticJavaPropertyAccessorKind {
    GETTER,
    SETTER;

    fun getJavaAccessorSymbol(propertySymbol: KaSyntheticJavaPropertySymbol): KaNamedFunctionSymbol? = when (this) {
        GETTER -> propertySymbol.javaGetterSymbol
        SETTER -> propertySymbol.javaSetterSymbol
    }

    fun getPropertyAccessorSymbol(propertySymbol: KaSyntheticJavaPropertySymbol): KaPropertyAccessorSymbol? = when (this) {
        GETTER -> propertySymbol.getter
        SETTER -> propertySymbol.setter
    }

    companion object {
        fun fromAccessorName(accessorName: Name): KaSyntheticJavaPropertyAccessorKind? {
            val accessorNameAsString = accessorName.asString()
            return when {
                JvmAbi.isGetterName(accessorNameAsString) -> GETTER
                JvmAbi.isSetterName(accessorNameAsString) -> SETTER
                else -> null
            }
        }
    }
}

@KaImplementationDetail
inline fun <T : KaFunctionSymbol> KaScope.findSyntheticJavaPropertyAccessor(
    accessorName: Name,
    selectAccessor: (KaSyntheticJavaPropertySymbol, KaSyntheticJavaPropertyAccessorKind, Name) -> T?,
): T? {
    val accessorKind = KaSyntheticJavaPropertyAccessorKind.fromAccessorName(accessorName) ?: return null
    for (propertyName in getPropertyNamesCandidatesByAccessorName(accessorName)) {
        for (callable in callables(propertyName)) {
            val propertySymbol = callable as? KaSyntheticJavaPropertySymbol ?: continue
            selectAccessor(propertySymbol, accessorKind, propertyName)?.let { return it }
        }
    }

    return null
}
