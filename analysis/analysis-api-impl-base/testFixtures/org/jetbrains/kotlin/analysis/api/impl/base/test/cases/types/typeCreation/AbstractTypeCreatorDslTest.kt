/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaCapturedTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.plugin.sandbox.fir.types.PluginFunctionalNames
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

/**
 * Test for type building DSL from [org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator].
 *
 * The test reuses the regular test infrastructure to retrieve types from expressions in the test data.
 * The test builds a `caret -> type` mapping from the test data and then runs a corresponding DSL test from [TestCases].
 * Each `caret` in the test file should mark a `KtExpression`, return type of which should be used in the type construction.
 *
 * If the test file is named `A.kt` and is placed inside `analysis/analysis-api/testData/types/typeCreation/byDsl/classType`,
 * then there should be a function called `testA` in the `ClassType` inner class in [TestCases], which retrieves types from
 * [TestCases.caretToType] mapping and returns some value constructed from them using the type-building DSL.
 * The returned value is then rendered in the output file `A.txt`.
 */
abstract class AbstractTypeCreatorDslTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actualText = analyzeForTest(mainFile) {
            val caretToType = testServices.expressionMarkerProvider.getAllCarets(mainFile).associate { caret ->
                val qualifier = caret.qualifier
                val expressionType = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtExpression>(
                    mainFile,
                    qualifier
                ).expressionType ?: error("Unable to retrieve type from `${caret.qualifier}` caret")

                qualifier to expressionType
            }

            val testName = testServices.testInfo.methodName
            val directoryName = testServices.testInfo.className.substringAfterLast("$")

            val result =
                TestCases(this@analyzeForTest, caretToType).runTest(directoryName, testName)

            render(result)
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }

    private fun KaSession.render(value: Any): String {
        val renderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
            this.capturedTypeRenderer = KaCapturedTypeRenderer.AS_CAPTURED_TYPE_WITH_PROJECTION
        }
        return buildString {
            when (value) {
                is KaType -> {
                    appendLine("KaType")
                    appendLine(DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, value))
                    appendLine()
                    appendLine("Rendered type:")
                    appendLine(value.render(renderer, position = Variance.INVARIANT))
                }
                is KaTypeArgumentWithVariance -> {
                    appendLine("KaTypeArgumentWithVariance")
                    appendLine("Variance: ${value.variance.name}")
                    appendLine("Type: ${DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, value.type)}")
                    appendLine()
                    appendLine("Rendered type argument:")
                    appendLine("${value.variance.name} ${value.type.render(renderer, position = Variance.INVARIANT)}")
                }
                is KaContextReceiver -> {
                    appendLine("KaContextReceiver")
                    appendLine("Label: ${value.label}")
                    appendLine("Type: ${DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, value.type)}")
                    appendLine()
                    appendLine("Rendered context receiver:")
                    appendLine("${value.label}: ${value.type.render(renderer, position = Variance.INVARIANT)}")
                }
                is KaFunctionValueParameter -> {
                    appendLine("KaFunctionValueParameter")
                    appendLine("Name: ${value.name}")
                    appendLine("Type: ${DebugSymbolRenderer(renderTypeByProperties = true).renderType(useSiteSession, value.type)}")
                    appendLine()
                    appendLine("Rendered function value parameter:")
                    appendLine("${value.name}: ${value.type.render(renderer, position = Variance.INVARIANT)}")
                }
                is KaStarTypeProjection -> {
                    appendLine("KaStarTypeProjection")
                }
                else -> error("Unable to render ${value::class}")
            }
        }
    }

    @Suppress("UNUSED")
    class TestCases(private val session: KaSession, private val caretToType: Map<String, KaType>) {
        fun runTest(directoryName: String, name: String): Any {
            val testClass = this::class.nestedClasses.single {
                it.simpleName == directoryName
            }

            val testClassInstance = testClass.primaryConstructor?.call(this) ?: error("Cannot construct test class for $directoryName")

            val testCase = testClass.members.filterIsInstance<KFunction<*>>().single { it.name == name }

            return testCase.call(testClassInstance) as Any
        }

        private fun getTypeByCaret(label: String): KaType {
            return caretToType[label] ?: error("No type for `$label`")
        }

        private fun getClassLikeSymbolByCaret(label: String): KaClassLikeSymbol {
            return caretToType[label]?.symbol ?: error("No symbol for `$label`")
        }

        private fun getTypeParameterSymbolByCaret(label: String): KaTypeParameterSymbol {
            return (caretToType[label] as? KaTypeParameterType)?.symbol ?: error("Type under `$label` is not a type parameter type")
        }

        inner class ClassType {
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
        }

        inner class DynamicType {
            fun testDynamicType(): KaType {
                return session.typeCreator.dynamicType()
            }

            fun testWithAnnotations(): KaType {
                val annotationClassId1 = ClassId.fromString("MyAnno1")
                val annotationClassId2 = ClassId.fromString("MyAnno2")
                val annotationClassId3 = ClassId.fromString("MyAnno3")

                return session.typeCreator.dynamicType {
                    annotation(annotationClassId1)
                    annotation(annotationClassId2)
                    annotation(annotationClassId3)
                }
            }
        }

        inner class StarTypeProjection {
            fun testStarTypeProjection(): KaTypeProjection {
                return session.typeCreator.starTypeProjection()
            }
        }

        inner class VarargArrayType {
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

        inner class TypeParameterType {
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

        inner class FunctionValueParameter {
            fun testNamedIntParameter(): KaFunctionValueParameter {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionValueParameter(Name.identifier("myName")) {
                    type
                }
            }

            fun testNullableStringType(): KaFunctionValueParameter {
                val symbol = getClassLikeSymbolByCaret("type")
                return session.typeCreator.functionValueParameter(Name.identifier("myName")) {
                    classType(symbol) {
                        isMarkedNullable = true
                    }
                }
            }

            fun testTypeParameterWithNoName(): KaFunctionValueParameter {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionValueParameter(null, type)
            }

            fun testVarargArrayType(): KaFunctionValueParameter {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionValueParameter(Name.identifier("myName")) {
                    varargArrayType(type)
                }
            }
        }

        inner class TypeArgumentWithVariance {
            fun testIntWithInVariance(): KaTypeArgumentWithVariance {
                val type = getTypeByCaret("type")
                return session.typeCreator.typeArgumentWithVariance(Variance.IN_VARIANCE, type)
            }

            fun testInvariantStringTypeMarkedNullable(): KaTypeArgumentWithVariance {
                val symbol = getClassLikeSymbolByCaret("type")
                return session.typeCreator.typeArgumentWithVariance(Variance.INVARIANT) {
                    classType(symbol) {
                        isMarkedNullable = true
                    }
                }
            }
        }

        inner class CapturedType {
            fun testFromAnotherCapturedTypeMarkedNullable(): KaType {
                val projection = session.typeCreator.starTypeProjection()
                val capturedType = session.typeCreator.capturedType(projection)
                return session.typeCreator.capturedType(capturedType) {
                    isMarkedNullable = true
                }
            }

            fun testOutIntProjection(): KaType {
                val type = getTypeByCaret("type")
                val projection = session.typeCreator.typeArgumentWithVariance(Variance.OUT_VARIANCE, type)
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
                val projection = session.typeCreator.typeArgumentWithVariance(Variance.IN_VARIANCE, type)
                return session.typeCreator.capturedType(projection)
            }

            fun testTypeParameterOutProjection(): KaType {
                val type = getTypeByCaret("type")
                val projection = session.typeCreator.typeArgumentWithVariance(Variance.OUT_VARIANCE, type)
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

        inner class ArrayType {
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

        inner class FlexibleType {
            fun testAnyAndNullableAny(): KaType {
                val lowerBound = getTypeByCaret("lower")
                val upperBound = getTypeByCaret("upper")
                return session.typeCreator.flexibleType(lowerBound, upperBound)
            }

            fun testIntAndNullableInt(): KaType {
                val lowerBound = getTypeByCaret("lower")
                val upperBound = getTypeByCaret("upper")
                return session.typeCreator.flexibleType(lowerBound, upperBound)
            }

            fun testNothingAndNullableAny(): KaType {
                val lowerBound = getTypeByCaret("lower")
                val upperBound = getTypeByCaret("upper")
                return session.typeCreator.flexibleType(lowerBound, upperBound)
            }

            fun testTwoUserTypes(): KaType {
                val lowerBound = getTypeByCaret("lower")
                val upperBound = getTypeByCaret("upper")
                return session.typeCreator.flexibleType(lowerBound, upperBound)
            }

            fun testTwoFlexibleTypes(): KaType {
                val lowerBound1 = getTypeByCaret("lower1")
                val upperBound1 = getTypeByCaret("upper1")
                val lowerBound2 = getTypeByCaret("lower2")
                val upperBound2 = getTypeByCaret("upper2")
                return session.typeCreator.flexibleType(
                    session.typeCreator.flexibleType(lowerBound1, upperBound1),
                    session.typeCreator.flexibleType(lowerBound2, upperBound2)
                )
            }

            fun testFlexibleTypeWithReplaceUpperBound(): KaType {
                val flexibleType = getTypeByCaret("type") as KaFlexibleType
                val upperbound = getTypeByCaret("upper")
                return session.typeCreator.flexibleType(flexibleType) {
                    upperBound = upperbound
                }
            }

            fun testWithAnnotations(): KaType {
                val annotationClassId1 = ClassId.fromString("MyAnno1")
                val annotationClassId2 = ClassId.fromString("MyAnno2")
                val annotationClassId3 = ClassId.fromString("MyAnno3")

                val lowerBound = getTypeByCaret("lower")
                val upperBound = getTypeByCaret("upper")
                return session.typeCreator.flexibleType(lowerBound, upperBound) {
                    annotations(listOf(annotationClassId1, annotationClassId2, annotationClassId3))
                }
            }
        }

        inner class DefinitelyNotNullType {
            fun testCapturedTypeIntOut(): KaType {
                val type = getTypeByCaret("type")
                with(session.typeCreator) {
                    val projection = typeArgumentWithVariance(Variance.OUT_VARIANCE, type)
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
                val type = getTypeByCaret("type")
                return session.typeCreator.definitelyNotNullType(type)
            }

            fun testTypeParameter(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.definitelyNotNullType(type)
            }

            fun testTypeParameterWithAnyUpperBound(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.definitelyNotNullType(type)
            }

            fun testTypeParameterWithNullableIntUpperBound(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.definitelyNotNullType(type)
            }

            fun testWithAnnotations(): KaType {
                val annotationClassId1 = ClassId.fromString("MyAnno1")
                val annotationClassId2 = ClassId.fromString("MyAnno2")
                val annotationClassId3 = ClassId.fromString("MyAnno3")

                val type = getTypeByCaret("type")
                return session.typeCreator.definitelyNotNullType(type) {
                    annotation { annotationClassId1 }
                    annotation(annotationClassId2)
                    annotations(listOf(annotationClassId3))
                }
            }
        }

        inner class IntersectionType {
            fun testSingleConjunct(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.intersectionType {
                    conjunct { type }
                }
            }

            fun testThreeRandomTypes(): KaType {
                val type1 = getTypeByCaret("1")
                val type2 = getTypeByCaret("2")
                val type3 = getTypeByCaret("3")
                return session.typeCreator.intersectionType {
                    conjunct { type1 }
                    conjunct(type2)
                    conjunct { type3 }
                }
            }

            fun testThreeSubtypes(): KaType {
                val type1 = getTypeByCaret("1")
                val type2 = getTypeByCaret("2")
                val type3 = getTypeByCaret("3")
                return session.typeCreator.intersectionType {
                    conjunct { type1 }
                    conjuncts {
                        listOf(type2, type3)
                    }
                }
            }

            fun testThreeUserSubtypes(): KaType {
                val type1 = getTypeByCaret("1")
                val type2 = getTypeByCaret("2")
                val type3 = getTypeByCaret("3")
                return session.typeCreator.intersectionType {
                    conjunct { type1 }
                    conjuncts(listOf(type2, type3))
                }
            }

            fun testWithError(): KaType {
                val type1 = getTypeByCaret("1")
                val type2 = getTypeByCaret("2")
                val type3 = getTypeByCaret("3")
                return session.typeCreator.intersectionType(listOf(type1, type2, type3))
            }

            fun testDuplicates(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.intersectionType(listOf(type, type, type))
            }

            fun testAnotherIntersectionTypeAsConjunct(): KaType {
                val type1 = getTypeByCaret("1")
                val type2 = getTypeByCaret("2")
                val type3 = getTypeByCaret("3")
                val type4 = getTypeByCaret("4")
                val intersection = session.typeCreator.intersectionType {
                    conjunct { type1 }
                    conjunct { type2 }
                    conjunct { type3 }
                }

                return session.typeCreator.intersectionType {
                    conjunct(intersection)
                    conjunct { type4 }
                }
            }

            fun testWithAnnotations(): KaType {
                val annotationClassId1 = ClassId.fromString("MyAnno1")
                val annotationClassId2 = ClassId.fromString("MyAnno2")
                val annotationClassId3 = ClassId.fromString("MyAnno3")

                val type = getTypeByCaret("type")
                return session.typeCreator.intersectionType {
                    conjunct { type }
                    annotation { annotationClassId1 }
                    annotation(annotationClassId2)
                    annotations(listOf(annotationClassId3))
                }
            }
        }

        inner class FunctionType {
            fun testBasicFunWithIntReturnType(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionType {
                    returnType = type
                }
            }

            fun testBasicFunWithIntReturnTypeAndReceiver(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionType {
                    returnType = type
                    receiverType = type
                }
            }

            fun testBasicNullableFunWithUserReturnType(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionType {
                    returnType = type
                    isMarkedNullable = true
                }
            }

            fun testWithDefaultValues(): KaType {
                return session.typeCreator.functionType()
            }

            fun testReflectWithDefaultValues(): KaType {
                return session.typeCreator.functionType {
                    isReflectType = true
                }
            }

            fun testSuspendWithDefaultValues(): KaType {
                return session.typeCreator.functionType {
                    isSuspend = true
                }
            }

            fun testReflectAndSuspendWithDefaultValues(): KaType {
                return session.typeCreator.functionType {
                    isReflectType = true
                    isSuspend = true
                }
            }

            fun testFourIntValueParameters(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionType {
                    valueParameter(Name.identifier("first"), type)
                    valueParameter(Name.identifier("second")) {
                        type
                    }
                    valueParameter {
                        functionValueParameter(Name.identifier("third")) {
                            type
                        }
                    }
                    valueParameter(functionValueParameter(Name.identifier("fourth"), type))
                }
            }

            fun testWithSingleContextParameter(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionType {
                    contextParameter {
                        type
                    }
                }
            }

            fun testReflectWithSingleContextParameter(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionType {
                    contextParameter {
                        type
                    }

                    isReflectType = true
                }
            }

            fun testWithContextParameterReceiverAndValueParameter(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionType {
                    contextParameter(type)
                    receiverType = type
                    valueParameter(Name.identifier("myParameter"), type)
                }
            }

            fun testWithPluginAnnotation(): KaType {
                val annotationClassId = PluginFunctionalNames.MY_INLINEABLE_ANNOTATION_CLASS_ID
                return session.typeCreator.functionType {
                    annotation(annotationClassId)
                }
            }

            fun testReflectWithStringReceiver(): KaType {
                val type = getTypeByCaret("type")
                return session.typeCreator.functionType {
                    receiverType = type
                    isReflectType = true
                }
            }

            fun testWithAnnotation(): KaType {
                val annotationClassId = ClassId.fromString("MyAnno")
                return session.typeCreator.functionType {
                    annotation { annotationClassId }
                }
            }

            fun testWithAnnotationReceiverAndContextParameter(): KaType {
                val annotationClassId = ClassId.fromString("MyAnno")
                val type = getTypeByCaret("type")
                return session.typeCreator.functionType {
                    receiverType = type
                    contextParameter { type }
                    annotation(annotationClassId)
                }
            }
        }
    }
}