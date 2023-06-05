/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.builder.FirClassBuilder
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class ClassWrapper(
    private val className: Name,
    private val modifiers: Modifier,
    private val classKind: ClassKind,
    val classBuilder: FirClassBuilder,
    val hasPrimaryConstructor: Boolean,
    val hasSecondaryConstructor: Boolean,
    val hasDefaultConstructor: Boolean,
    val delegatedSelfTypeRef: FirTypeRef,
    val delegatedSuperTypeRef: FirTypeRef,
    val delegatedSuperCalls: List<DelegatedConstructorWrapper>,
) {
    fun isObjectLiteral(): Boolean {
        return className == SpecialNames.NO_NAME_PROVIDED && isObject()
    }

    fun isEnumEntry(): Boolean {
        return classKind == ClassKind.ENUM_ENTRY
    }

    private fun isObject(): Boolean {
        return classKind == ClassKind.OBJECT
    }

    fun isSealed(): Boolean {
        return modifiers.hasModality(Modality.SEALED)
    }

    fun isEnum(): Boolean {
        return modifiers.isEnum()
    }

    fun isInterface(): Boolean {
        return classKind == ClassKind.INTERFACE
    }

    fun isInner(): Boolean {
        return modifiers.isInner()
    }

    fun hasExpect(): Boolean {
        return modifiers.hasExpect()
    }

    // See DescriptorUtils#getDefaultConstructorVisibility in core.descriptors
    fun defaultConstructorVisibility(): Visibility {
        return when {
            isObject() || isEnum() || isEnumEntry() -> Visibilities.Private
            isSealed() -> Visibilities.Protected
            else -> Visibilities.Unknown
        }
    }
}

data class DelegatedConstructorWrapper(
    val delegatedSuperTypeRef: FirTypeRef,
    val arguments: List<FirExpression>,
    val source: KtLightSourceElement?,
)
