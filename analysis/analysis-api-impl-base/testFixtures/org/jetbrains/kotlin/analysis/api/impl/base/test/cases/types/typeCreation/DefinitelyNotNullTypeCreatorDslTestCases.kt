/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

@Suppress("UNUSED")
class DefinitelyNotNullTypeCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testCapturedTypeIntOut(): KaType {
        val type = getTypeByCaret("type")
        with(session.typeCreator) {
            val projection = typeProjection(Variance.OUT_VARIANCE, type)
            val capturedType = capturedType(projection)
            return definitelyNotNullType(capturedType)
        }
    }

    fun testCapturedTypeWithStarProjection(): KaType {
        with(session.typeCreator) {
            val projection = starTypeProjection()
            val capturedType = capturedType(projection)
            return definitelyNotNullType(capturedType)
        }
    }

    fun testNullableTypeParameter(): KaType {
        val type = getTypeByCaret("type") as KaTypeParameterType? ?: error("Type under `type` is not a type parameter")
        return session.typeCreator.definitelyNotNullType(type)
    }

    fun testTypeParameter(): KaType {
        val type = getTypeByCaret("type") as KaTypeParameterType? ?: error("Type under `type` is not a type parameter")
        return session.typeCreator.definitelyNotNullType(type)
    }

    fun testTypeParameterWithAnyUpperBound(): KaType {
        val type = getTypeByCaret("type") as KaTypeParameterType? ?: error("Type under `type` is not a type parameter")
        return session.typeCreator.definitelyNotNullType(type)
    }

    fun testTypeParameterWithNullableIntUpperBound(): KaType {
        val type = getTypeByCaret("type") as KaTypeParameterType? ?: error("Type under `type` is not a type parameter")
        return session.typeCreator.definitelyNotNullType(type)
    }

    fun testWithAnnotations(): KaType {
        val annotationClassId1 = ClassId.fromString("MyAnno1")
        val annotationClassId2 = ClassId.fromString("MyAnno2")
        val annotationClassId3 = ClassId.fromString("MyAnno3")

        val type = getTypeByCaret("type") as KaTypeParameterType? ?: error("Type under `type` is not a type parameter")
        return session.typeCreator.definitelyNotNullType(type) {
            annotation { annotationClassId1 }
            annotation(annotationClassId2)
            annotations(listOf(annotationClassId3))
        }
    }

    fun testCapturedNullableTypeOutProjection(): KaType {
        val type = getTypeByCaret("type")
        with(session.typeCreator) {
            val projection = typeProjection(Variance.OUT_VARIANCE, type)
            val capturedType = capturedType(projection)
            return definitelyNotNullType(capturedType)
        }
    }
}