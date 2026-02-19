/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType

@Suppress("UNUSED")
class VarargArrayTypeCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testBoxedArray(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.varargArrayType(type)
    }

    fun testErrorType(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.varargArrayType(type)
    }

    fun testFlexibleInt(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.varargArrayType(type)
    }

    fun testNullableInt(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.varargArrayType(type)
    }

    fun testNullableUserType(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.varargArrayType(type)
    }

    fun testPrimitiveArray(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.varargArrayType(type)
    }

    fun testSimpleUserType(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.varargArrayType(type)
    }

    fun testTypeParameter(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.varargArrayType(type)
    }

    fun testTypeParameterWithIntUpperBound(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.varargArrayType(type)
    }
}