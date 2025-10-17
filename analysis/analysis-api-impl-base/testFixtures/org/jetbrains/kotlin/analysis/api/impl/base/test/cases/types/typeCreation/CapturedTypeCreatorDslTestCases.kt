/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

@Suppress("UNUSED")
class CapturedTypeCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testFromAnotherCapturedTypeMarkedNullable(): KaType {
        val projection = session.typeCreator.starTypeProjection()
        val capturedType = session.typeCreator.capturedType(projection)
        return session.typeCreator.capturedType(capturedType) {
            isMarkedNullable = true
        }
    }

    fun testOutIntProjection(): KaType {
        val type = getTypeByCaret("type")
        val projection = session.typeCreator.typeProjection(Variance.OUT_VARIANCE, type)
        return session.typeCreator.capturedType(projection)
    }

    fun testStarProjection(): KaType {
        val projection = session.typeCreator.starTypeProjection()
        return session.typeCreator.capturedType(projection)
    }

    fun testStarProjectionMarkedNullable(): KaType {
        val projection = session.typeCreator.starTypeProjection()
        return session.typeCreator.capturedType(projection) {
            isMarkedNullable = true
        }
    }

    fun testUserTypeInProjection(): KaType {
        val type = getTypeByCaret("type")
        val projection = session.typeCreator.typeProjection(Variance.IN_VARIANCE, type)
        return session.typeCreator.capturedType(projection)
    }

    fun testTypeParameterOutProjection(): KaType {
        val type = getTypeByCaret("type")
        val projection = session.typeCreator.typeProjection(Variance.OUT_VARIANCE, type)
        return session.typeCreator.capturedType(projection)
    }

    fun testStarProjectionWithAnnotations(): KaType {
        val annotationClassId1 = ClassId.fromString("MyAnno1")
        val annotationClassId2 = ClassId.fromString("MyAnno2")
        val annotationClassId3 = ClassId.fromString("MyAnno3")

        val projection = session.typeCreator.starTypeProjection()
        return session.typeCreator.capturedType(projection) {
            annotations(listOf(annotationClassId1, annotationClassId2, annotationClassId3))
        }
    }
}