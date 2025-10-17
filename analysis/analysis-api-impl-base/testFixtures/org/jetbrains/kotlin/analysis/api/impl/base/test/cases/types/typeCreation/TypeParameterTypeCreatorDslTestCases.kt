/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId

@Suppress("UNUSED")
class TypeParameterTypeCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testRegularMakeNullable(): KaType {
        val symbol = getTypeParameterSymbolByCaret("type")
        return session.typeCreator.typeParameterType(symbol) {
            isMarkedNullable = true
        }
    }

    fun testReified(): KaType {
        val symbol = getTypeParameterSymbolByCaret("type")
        return session.typeCreator.typeParameterType(symbol)
    }

    fun testWithUpperBounds(): KaType {
        val symbol = getTypeParameterSymbolByCaret("type")
        return session.typeCreator.typeParameterType(symbol)
    }

    fun testWithAnnotations(): KaType {
        val annotationClassId1 = ClassId.fromString("MyAnno1")
        val annotationClassId2 = ClassId.fromString("MyAnno2")
        val annotationClassId3 = ClassId.fromString("MyAnno3")

        val symbol = getTypeParameterSymbolByCaret("type")
        return session.typeCreator.typeParameterType(symbol) {
            annotation(annotationClassId1)
            annotation(annotationClassId2)
            annotation(annotationClassId3)
        }
    }
}