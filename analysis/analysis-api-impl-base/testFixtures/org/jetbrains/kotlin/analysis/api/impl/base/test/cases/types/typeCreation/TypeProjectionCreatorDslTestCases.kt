/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.types.Variance

@Suppress("UNUSED")
class TypeProjectionCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testIntWithInVariance(): KaTypeArgumentWithVariance {
        val type = getTypeByCaret("type")
        return session.typeCreator.typeProjection(Variance.IN_VARIANCE, type)
    }

    fun testInvariantStringTypeMarkedNullable(): KaTypeArgumentWithVariance {
        val symbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.typeProjection(Variance.INVARIANT) {
            classType(symbol) {
                isMarkedNullable = true
            }
        }
    }
}