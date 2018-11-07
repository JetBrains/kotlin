/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.expressions.FirAnnotationContainer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

interface FirMemberDeclaration : FirTypeParameterContainer, FirNamedDeclaration, FirAnnotationContainer {
    val visibility: Visibility

    val modality: Modality?

    val platformStatus: FirMemberPlatformStatus

    val isExpect: Boolean get() = platformStatus == FirMemberPlatformStatus.EXPECT

    val isActual: Boolean get() = platformStatus == FirMemberPlatformStatus.ACTUAL

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitMemberDeclaration(this, data)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        acceptAnnotations(visitor, data)
        for (typeParameter in typeParameters) {
            typeParameter.accept(visitor, data)
        }
        super.acceptChildren(visitor, data)
    }
}

enum class FirMemberPlatformStatus {
    DEFAULT,
    EXPECT,
    ACTUAL
}