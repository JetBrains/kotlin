/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

@Suppress("UNUSED")
class ClassTypeCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    DslTypeCreationTestCases(session, caretToType) {
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

    /**
     * The anonymous object captures `T` from `box`, so its type at the call site is `<anonymous><String>`.
     * The type is not created by the DSL, but taken as is, as it is the source of the copy tests.
     */
    fun testAnonymousObjectFromGenericFunction(): KaType {
        return getTypeByCaret("type")
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
        return session.typeCreator.classType(ClassId(FqName.ROOT, Name.identifier("MyClass"))) {
            isMarkedNullable = true
            annotation(ClassId.fromString("Anno"))
            invariantTypeArgument(session.builtinTypes.int)
            outerClasses { index, symbol ->
                invariantTypeArgument(session.builtinTypes.string)
            }
        }
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

    fun testUserTypeWithAttributeAnnotations(): KaType {
        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol) {
            annotations(ATTRIBUTE_ANNOTATION_CLASS_IDS)
        }
    }

    fun testFunctionClassWithAttributeAnnotations(): KaType {
        val stringType = getTypeByCaret("string")
        val intType = getTypeByCaret("int")
        return session.typeCreator.classType(StandardClassIds.FunctionN(1)) {
            invariantTypeArgument(stringType)
            invariantTypeArgument(intType)
            annotations(ATTRIBUTE_ANNOTATION_CLASS_IDS)
        }
    }

    fun testFunctionTypeAliasWithAttributeAnnotations(): KaType {
        val alias = getClassLikeSymbolByCaret("alias")
        return session.typeCreator.classType(alias) {
            annotations(ATTRIBUTE_ANNOTATION_CLASS_IDS)
        }
    }

    fun testUserTypeWithPositiveAnnotation(): KaType {
        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol) {
            annotation(ClassId.fromString("org/jetbrains/kotlin/plugin/sandbox/Positive"))
        }
    }

    fun testUserTypeWithNegativeAnnotation(): KaType {
        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol) {
            annotation(ClassId.fromString("org/jetbrains/kotlin/plugin/sandbox/Negative"))
        }
    }

    fun testUserTypeWithPositiveAndNegativeAnnotations(): KaType {
        val userTypeSymbol = getClassLikeSymbolByCaret("type")
        return session.typeCreator.classType(userTypeSymbol) {
            annotation(ClassId.fromString("org/jetbrains/kotlin/plugin/sandbox/Positive"))
            annotation(ClassId.fromString("org/jetbrains/kotlin/plugin/sandbox/Negative"))
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

    fun testInnerClassWithOuterTypeArguments(): KaType {
        val innerTypeSymbol = getClassLikeSymbolByCaret("type")

        return session.typeCreator.classType(innerTypeSymbol) {
            invariantTypeArgument(getTypeByCaret("long"))
            invariantTypeArgument(getTypeByCaret("double"))

            outerClasses { index, symbol ->
                when (symbol.name?.asString()) {
                    "Middle" -> {
                        invariantTypeArgument(getTypeByCaret("boolean"))
                        invariantTypeArgument(getTypeByCaret("char"))
                    }
                }
            }

            outerClasses { index, symbol ->
                when (symbol.name?.asString()) {
                    "Outer" -> invariantTypeArgument(getTypeByCaret("long"))
                }
            }
        }
    }

    fun testMissingDependencyCandidateSymbol(): KaType {
        val errorType = getTypeByCaret("type") as KaClassErrorType
        val symbol = errorType.candidateSymbols.single()
        return session.typeCreator.classType(symbol) {
            isMarkedNullable = true
            annotation(ClassId.fromString("main/Anno"))
            invariantTypeArgument(session.builtinTypes.int)
            outerClasses { index, symbol ->
                invariantTypeArgument(session.builtinTypes.string)
            }
        }
    }

    /**
     * The candidate symbol is `base.Foo` from the `base` module, which has no type parameters.
     * Its class ID resolves to a different `base.Foo<T>` from the use-site module, which has one type parameter.
     */
    fun testMissingDependencyCandidateSymbolWithClashingUseSiteClass(): KaType {
        val errorType = getTypeByCaret("type") as KaClassErrorType
        val symbol = errorType.candidateSymbols.single()
        return session.typeCreator.classType(symbol)
    }

    fun testInnerClassOfGenericClass(): KaType {
        val innerTypeSymbol = getClassLikeSymbolByCaret("type")

        return session.typeCreator.classType(innerTypeSymbol) {
            invariantTypeArgument(getTypeByCaret("string"))
            outerClasses { index, _ ->
                check(index == 0)
                invariantTypeArgument(getTypeByCaret("int"))
            }
        }.checkSameAsTypeAtCaret()
    }

    fun testMixedInnerAndNestedClasses(): KaType {
        val innerTypeSymbol = getClassLikeSymbolByCaret("type")

        return session.typeCreator.classType(innerTypeSymbol) {
            invariantTypeArgument(getTypeByCaret("string"))
            outerClasses { index, symbol ->
                when (index) {
                    // `Outer` is nested, so the type arguments of `Top` are ignored
                    0 -> invariantTypeArgument(getTypeByCaret("string"))
                    1 -> invariantTypeArgument(getTypeByCaret("int"))
                    // `Middle` has no type parameters
                    2 -> invariantTypeArgument(getTypeByCaret("string"))
                    else -> error("Unexpected outer class $symbol at $index")
                }
            }
        }.checkSameAsTypeAtCaret()
    }

    fun testInnerClassOfLocalClass(): KaType {
        val innerTypeSymbol = getClassLikeSymbolByCaret("type")

        return session.typeCreator.classType(innerTypeSymbol) {
            invariantTypeArgument(getTypeByCaret("string"))
            // The type parameter `F` captured from the containing function is used as its own argument
            outerClasses { index, _ ->
                check(index == 0)
                invariantTypeArgument(getTypeByCaret("int"))
            }
        }.checkSameAsTypeAtCaret()
    }

    fun testInnerClassOfLocalClassNoArgs(): KaType {
        val innerTypeSymbol = getClassLikeSymbolByCaret("type")

        return session.typeCreator.classType(innerTypeSymbol)
    }

    fun testInnerClassOfLocalClassInGenericMember(): KaType {
        val innerTypeSymbol = getClassLikeSymbolByCaret("type")

        return session.typeCreator.classType(innerTypeSymbol) {
            invariantTypeArgument(getTypeByCaret("string"))
            // The qualifiers stop at the local class, so `A` is not visited.
            // The type parameters `F` and `AA` captured from the containing declarations are used as their own arguments
            outerClasses { index, symbol ->
                check(index == 0 && symbol.name?.asString() == "Local") { "Unexpected outer class $symbol at $index" }
                invariantTypeArgument(getTypeByCaret("int"))
            }
        }.checkSameAsTypeAtCaret()
    }

    fun testNestedClassOfGenericClass(): KaType {
        val nestedTypeSymbol = getClassLikeSymbolByCaret("type")

        return session.typeCreator.classType(nestedTypeSymbol) {
            invariantTypeArgument(getTypeByCaret("string"))
            // `Nested` is not inner, so the type arguments of `Outer` are ignored
            outerClasses { _, _ -> invariantTypeArgument(getTypeByCaret("string")) }
        }.checkSameAsTypeAtCaret()
    }

    fun testCompanionOfGenericClass(): KaType {
        val companionTypeSymbol = getClassLikeSymbolByCaret("type")

        return session.typeCreator.classType(companionTypeSymbol) {
            repeat(10) {
                invariantTypeArgument(arrayType(dynamicType()))
            }

            outerClasses { _, _ ->
                repeat(10) {
                    invariantTypeArgument(dynamicType())
                }
            }
        }
    }

    fun testCompanionOfGenericClassNoArgs(): KaType {
        val companionTypeSymbol = getClassLikeSymbolByCaret("type")

        return session.typeCreator.classType(companionTypeSymbol)
    }

    /**
     * Checks that the created type is exactly the type at the `type` caret, including the type arguments
     * that the [qualifiers][org.jetbrains.kotlin.analysis.api.types.KaClassType.qualifiers] do not show.
     */
    private fun KaType.checkSameAsTypeAtCaret(): KaType = apply {
        val expectedType = getTypeByCaret("type")
        check(this == expectedType) { "The created type `$this` differs from the type at the caret `$expectedType`" }
    }
}
