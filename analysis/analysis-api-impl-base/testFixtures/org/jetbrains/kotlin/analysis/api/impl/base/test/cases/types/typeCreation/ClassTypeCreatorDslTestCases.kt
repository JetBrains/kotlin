/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

@Suppress("UNUSED")
class ClassTypeCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testIntTypeMarkNullable(): KaType {
        val intTypeSymbol = getClassLikeSymbolByCaret("int")
        return session.typeCreator.classType(intTypeSymbol) {
            isMarkedNullable = true
        }
    }

    fun testUserType(): KaType {
        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol)
    }

    fun testLocalUserType(): KaType {
        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol)
    }

    fun testBoxedArrayWithStringTypeArgument(): KaType {
        val arrayTypeSymbol = getClassLikeSymbolByCaret("array")
        val stringType = getTypeByCaret("string")
        return session.typeCreator.classType(arrayTypeSymbol) {
            typeArgument(Variance.IN_VARIANCE) {
                stringType
            }
        }
    }

    fun testMoreTypeArgumentsThanExpected(): KaType {
        val arrayTypeSymbol = getClassLikeSymbolByCaret("array")
        val stringType = getTypeByCaret("string")
        return session.typeCreator.classType(arrayTypeSymbol) {
            typeArgument(Variance.IN_VARIANCE) {
                stringType
            }
            typeArgument(Variance.OUT_VARIANCE) {
                stringType
            }
            invariantTypeArgument(stringType)
        }
    }

    fun testLessTypeArgumentsThanExpected(): KaType {
        val arrayTypeSymbol = getClassLikeSymbolByCaret("array")
        return session.typeCreator.classType(arrayTypeSymbol)
    }

    fun testNonExistingClassId(): KaType {
        return session.typeCreator.classType(ClassId(FqName.ROOT, Name.identifier("MyClass")))
    }

    fun testUserGenericTypeWithStarProjection(): KaType {
        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol) {
            typeArgument(starTypeProjection())
        }
    }

    fun testNonExistingClassIdWithAnnotations(): KaType {
        val annotationClassId1 = ClassId.fromString("MyAnno1")
        val annotationClassId2 = ClassId.fromString("MyAnno2")
        val annotationClassId3 = ClassId.fromString("MyAnno3")

        return session.typeCreator.classType(ClassId(FqName.ROOT, Name.identifier("MyClass"))) {
            annotation(annotationClassId1)
            annotation(annotationClassId2)
            annotation(annotationClassId3)
        }
    }

    fun testUserTypeWithAnnotations(): KaType {
        val annotationClassId1 = ClassId.fromString("MyAnno1")
        val annotationClassId2 = ClassId.fromString("MyAnno2")
        val annotationClassId3 = ClassId.fromString("MyAnno3")

        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol) {
            annotation(annotationClassId1)
            annotation(annotationClassId2)
            annotation(annotationClassId3)
        }
    }

    fun testWithAnnotationRequiringArguments(): KaType {
        val annotationClassId = ClassId.fromString("MyAnno")

        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol) {
            annotation(annotationClassId)
        }
    }

    fun testWithGenericAnnotation(): KaType {
        val annotationClassId = ClassId.fromString("MyAnno")

        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol) {
            annotation(annotationClassId)
        }
    }

    fun testGenericTypeAliasWithIntArgument(): KaType {
        val alias = getClassLikeSymbolByCaret("alias")
        val argument = getTypeByCaret("argument")

        return session.typeCreator.classType(alias) {
            invariantTypeArgument(argument)
        }
    }

    fun testGenericTypeAliasWithNoArguments(): KaType {
        val alias = getClassLikeSymbolByCaret("alias")

        return session.typeCreator.classType(alias)
    }

    fun testTypeAlias(): KaType {
        val alias = getClassLikeSymbolByCaret("alias")

        return session.typeCreator.classType(alias)
    }

    fun testTypeAliasWithNestedTypeAliases(): KaType {
        val alias = getClassLikeSymbolByCaret("alias")

        return session.typeCreator.classType(alias)
    }

    fun testListWithTypeAliasArgument(): KaType {
        val alias = getTypeByCaret("alias")
        return session.typeCreator.classType(StandardClassIds.List) {
            invariantTypeArgument(alias)
        }
    }
}