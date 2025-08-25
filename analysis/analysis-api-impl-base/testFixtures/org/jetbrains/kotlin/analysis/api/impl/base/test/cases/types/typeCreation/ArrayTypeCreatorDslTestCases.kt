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
class ArrayTypeCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testBoolPreferPrimitive(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            shouldPreferPrimitiveTypes = true
        }
    }

    fun testBoxedArrayOutVariance(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            variance = Variance.OUT_VARIANCE
        }
    }

    fun testErrorType(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type)
    }

    fun testFlexibleInt(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type)
    }

    fun testIntOutVariance(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            variance = Variance.OUT_VARIANCE
            shouldPreferPrimitiveTypes = false
        }
    }

    fun testNullableIntPreferPrimitive(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            shouldPreferPrimitiveTypes = true
        }
    }

    fun testNullableUserType(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type)
    }

    fun testPrimitiveArrayPreferPrimitive(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            shouldPreferPrimitiveTypes = true
        }
    }

    fun testSimpleUserTypeMakeNullablePreferPrimitive(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            shouldPreferPrimitiveTypes = true
            isMarkedNullable = true
        }
    }

    fun testTypeParameterPreferPrimitiveOutVariance(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            shouldPreferPrimitiveTypes = true
            variance = Variance.OUT_VARIANCE
        }
    }

    fun testTypeParameterWithIntUpperBound(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type)
    }

    fun testCharShouldNotPreferPrimitive(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            shouldPreferPrimitiveTypes = false
        }
    }

    fun testIntInVarianceShouldPreferPrimitive(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            shouldPreferPrimitiveTypes = true
            variance = Variance.IN_VARIANCE
        }
    }

    fun testInt(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type)
    }

    fun testDynamicType(): KaType {
        val type = session.typeCreator.dynamicType()
        return session.typeCreator.arrayType(type)
    }

    fun testWithAnnotations(): KaType {
        val annotationClassId1 = ClassId.fromString("MyAnno1")
        val annotationClassId2 = ClassId.fromString("MyAnno2")
        val annotationClassId3 = ClassId.fromString("MyAnno3")

        val type = getTypeByCaret("type")
        return session.typeCreator.arrayType(type) {
            annotations(listOf(annotationClassId1, annotationClassId2, annotationClassId3))
        }
    }
}