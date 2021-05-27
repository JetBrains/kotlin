/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import javax.swing.Icon

internal object KotlinFirIconProvider {
    fun getIconFor(symbol: KtSymbol): Icon? {
        if (symbol is KtFunctionSymbol) {
            val isAbstract = symbol.modality == Modality.ABSTRACT

            return when {
                symbol.isExtension -> {
                    if (isAbstract) KotlinIcons.ABSTRACT_EXTENSION_FUNCTION else KotlinIcons.EXTENSION_FUNCTION
                }
                symbol.symbolKind == KtSymbolKind.MEMBER -> {
                    if (isAbstract) PlatformIcons.ABSTRACT_METHOD_ICON else PlatformIcons.METHOD_ICON
                }
                else -> KotlinIcons.FUNCTION
            }
        }

        if (symbol is KtClassOrObjectSymbol) {
            val isAbstract = (symbol as? KtNamedClassOrObjectSymbol)?.modality == Modality.ABSTRACT

            return when (symbol.classKind) {
                KtClassKind.CLASS -> if (isAbstract) KotlinIcons.ABSTRACT_CLASS else KotlinIcons.CLASS
                KtClassKind.ENUM_CLASS, KtClassKind.ENUM_ENTRY -> KotlinIcons.ENUM
                KtClassKind.ANNOTATION_CLASS -> KotlinIcons.ANNOTATION
                KtClassKind.OBJECT, KtClassKind.COMPANION_OBJECT -> KotlinIcons.OBJECT
                KtClassKind.INTERFACE -> KotlinIcons.INTERFACE
                KtClassKind.ANONYMOUS_OBJECT -> KotlinIcons.OBJECT
            }
        }

        if (symbol is KtValueParameterSymbol) return KotlinIcons.PARAMETER

        if (symbol is KtLocalVariableSymbol) return if (symbol.isVal) KotlinIcons.VAL else KotlinIcons.VAR

        if (symbol is KtPropertySymbol) return if (symbol.isVal) KotlinIcons.FIELD_VAL else KotlinIcons.FIELD_VAR

        if (symbol is KtTypeParameterSymbol) return PlatformIcons.CLASS_ICON

        if (symbol is KtTypeAliasSymbol) return KotlinIcons.TYPE_ALIAS

        if (symbol is KtEnumEntrySymbol) return KotlinIcons.ENUM

        return null
    }
}