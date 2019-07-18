/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirUserTypeRefImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class ClassWrapper(
    private val session: FirSession,
    private val className: Name,
    private val modifiers: Modifier,
    private val classKind: ClassKind,
    val hasPrimaryConstructor: Boolean,
    val hasSecondaryConstructor: Boolean,
    var delegatedSelfTypeRef: FirTypeRef,
    val delegatedSuperTypeRef: FirTypeRef,
    val superTypeCallEntry: MutableList<FirExpression>
) {
    init {
        if (className == SpecialNames.NO_NAME_PROVIDED) {
            delegatedSelfTypeRef = delegatedSuperTypeRef
        }
    }

    private fun isObject(): Boolean {
        return classKind == ClassKind.OBJECT
    }

    private fun isSealed(): Boolean {
        return modifiers.getModality() == Modality.SEALED
    }

    private fun isEnum(): Boolean {
        return modifiers.isEnum()
    }

    fun isInterface(): Boolean {
        return classKind == ClassKind.INTERFACE
    }

    fun defaultConstructorVisibility(): Visibility {
        return if (isObject() || isSealed() || isEnum())
            Visibilities.PRIVATE
        else
            Visibilities.UNKNOWN
    }

    fun getFirUserTypeFromClassName(): FirUserTypeRef {
        val qualifier = FirQualifierPartImpl(
            className
        )

        return FirUserTypeRefImpl(
            session,
            null,
            false
        ).apply {
            this.qualifier.add(qualifier)
        }
    }
}