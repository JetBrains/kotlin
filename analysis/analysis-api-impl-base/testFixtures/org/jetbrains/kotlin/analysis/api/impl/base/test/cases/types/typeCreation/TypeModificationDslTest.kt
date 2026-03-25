/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.base.AnalysisApiExecutionTestEnvironment
import org.jetbrains.kotlin.analysis.test.framework.base.AnalysisApiTestEnvironmentStorage
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.testInfo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.fail

@OptIn(KaExperimentalApi::class)
@TestMetadata("analysis/analysis-api/testData/types/typeCreation/byDsl")
@TestDataPath($$"$PROJECT_ROOT")
@Disabled("Only the concrete subclasses of 'AbstractTypeModificationDslTest' should run")
abstract class AbstractTypeModificationDslTest : AbstractTypeModificationDslTestBase("../analysis-api/testData/types/typeCreation/byDsl") {

    // region KaClassType modifications

    @Test
    @TestMetadata("classType/intTypeMarkNullable.kt")
    fun `classType intTypeMarkNullable +markNotNull`() = test<KaUsualClassType> { type ->
        type.copy {
            isMarkedNullable = false
        }
    }

    @Test
    @TestMetadata("classType/intTypeMarkNullable.kt")
    fun `classType intTypeMarkNullable +markNullableOnceAgain`() = test<KaClassType> { type ->
        type.copy {
            isMarkedNullable = true
        }
    }

    @Test
    @TestMetadata("classType/userType.kt")
    fun `classType userType +markNullable`() = test<KaClassType> { type ->
        type.copy {
            isMarkedNullable = true
        }
    }

    @Test
    @TestMetadata("classType/userType.kt")
    fun `classType userType +addAnnotation`() = test<KaUsualClassType> { type ->
        type.copy {
            annotation(ClassId.fromString("MyAnno"))
        }
    }

    @Test
    @TestMetadata("classType/userType.kt")
    fun `classType userType +addNonExistentAnnotation`() = test<KaUsualClassType> { type ->
        type.copy {
            annotation(ClassId.fromString("NonExistentAnno"))
        }
    }

    @Test
    @TestMetadata("classType/userTypeWithAnnotations.kt")
    fun `classType userTypeWithAnnotations +markNullable`() = test<KaClassType> { type ->
        type.copy {
            isMarkedNullable = true
        }
    }

    @Test
    @TestMetadata("classType/userTypeWithAnnotations.kt")
    fun `classType userTypeWithAnnotations +addAnnotation`() = test<KaClassType> { type ->
        type.copy {
            annotation(ClassId.fromString("MyAnno4"))
        }
    }

    @Test
    @TestMetadata("classType/boxedArrayWithStringTypeArgument.kt")
    fun `classType boxedArrayWithStringTypeArgument +markNullable`() = test<KaClassType> { type ->
        type.copy {
            isMarkedNullable = true
        }
    }

    @Test
    @TestMetadata("classType/boxedArrayWithStringTypeArgument.kt")
    fun `classType boxedArrayWithStringTypeArgument +addTypeArgument`() = test<KaClassType> { type ->
        type.copy {
            typeArgument(starTypeProjection())
        }
    }

    @Test
    @TestMetadata("classType/typeAlias.kt")
    fun `classType typeAlias +markNullable`() = test<KaClassType> { type ->
        type.copy {
            isMarkedNullable = true
        }
    }

    @Test
    @TestMetadata("classType/typeAlias.kt")
    fun `classType typeAlias +addTypeAliasAnnotation`() = test<KaClassType> { type ->
        type.copy {
            annotation(ClassId.fromString("/MyAlias"))
        }
    }

    @Test
    @TestMetadata("classType/genericTypeAliasWithIntArgument.kt")
    fun `classType genericTypeAliasWithIntArgument +markNullable`() = test<KaUsualClassType> { type ->
        type.copy {
            isMarkedNullable = true
        }
    }

    // endregion

    // region KaFunctionType modifications

    @Test
    @TestMetadata("functionType/basicFunWithIntReturnType.kt")
    fun `functionType basicFunWithIntReturnType +markNullable`() = test<KaFunctionType> { type ->
        type.copy {
            isMarkedNullable = true
        }
    }

    @Test
    @TestMetadata("functionType/basicFunWithIntReturnType.kt")
    fun `functionType basicFunWithIntReturnType +makeSuspend`() = test<KaFunctionType> { type ->
        type.copy {
            isSuspend = true
        }
    }

    @Test
    @TestMetadata("functionType/basicFunWithIntReturnType.kt")
    fun `functionType basicFunWithIntReturnType +makeReflect`() = test<KaFunctionType> { type ->
        type.copy {
            isReflectType = true
        }
    }

    @Test
    @TestMetadata("functionType/basicNullableFunWithUserReturnType.kt")
    fun `functionType basicNullableFunWithUserReturnType +markNotNull`() = test<KaFunctionType> { type ->
        type.copy {
            isMarkedNullable = false
        }
    }

    @Test
    @TestMetadata("functionType/suspendWithDefaultValues.kt")
    fun `functionType suspendWithDefaultValues +removeSuspend`() = test<KaFunctionType> { type ->
        type.copy {
            isSuspend = false
        }
    }

    @Test
    @TestMetadata("functionType/reflectWithDefaultValues.kt")
    fun `functionType reflectWithDefaultValues +removeReflect`() = test<KaFunctionType> { type ->
        type.copy {
            isReflectType = false
        }
    }

    @Test
    @TestMetadata("functionType/reflectAndSuspendWithDefaultValues.kt")
    fun `functionType reflectAndSuspendWithDefaultValues +removeSuspendAndReflect`() = test<KaFunctionType> { type ->
        type.copy {
            isSuspend = false
            isReflectType = false
        }
    }

    @Test
    @TestMetadata("functionType/fourIntValueParameters.kt")
    fun `functionType fourIntValueParameters +addReceiver`() = test<KaFunctionType> { type ->
        type.copy {
            receiverType = classType(StandardClassIds.String)
        }
    }

    @Test
    @TestMetadata("functionType/basicFunWithIntReturnTypeAndReceiver.kt")
    fun `functionType basicFunWithIntReturnTypeAndReceiver +removeReceiver`() = test<KaFunctionType> { type ->
        type.copy {
            receiverType = null
        }
    }

    @Test
    @TestMetadata("functionType/withContextParameterReceiverAndValueParameter.kt")
    fun `functionType withContextParameterReceiverAndValueParameter +makeSuspend`() = test<KaFunctionType> { type ->
        type.copy {
            isSuspend = true
        }
    }

    @Test
    @TestMetadata("functionType/basicFunWithIntReturnType.kt")
    fun `functionType basicFunWithIntReturnType +addAnnotation`() = test<KaFunctionType> { type ->
        type.copy {
            annotation(ClassId.fromString("Anno"))
        }
    }

    @Test
    @TestMetadata("functionType/basicFunWithIntReturnType.kt")
    fun `functionType basicFunWithIntReturnType +addContextParameter`() = test<KaFunctionType> { type ->
        type.copy {
            contextParameter(classType(StandardClassIds.String))
        }
    }

    @Test
    @TestMetadata("functionType/withSingleContextParameter.kt")
    fun `functionType withSingleContextParameter +makeSuspend`() = test<KaFunctionType> { type ->
        type.copy {
            isSuspend = true
        }
    }

    @Test
    @TestMetadata("functionType/basicFunWithIntReturnType.kt")
    fun `functionType basicFunWithIntReturnType +changeReturnType`() = test<KaFunctionType> { type ->
        type.copy {
            returnType = classType(StandardClassIds.String)
        }
    }

    // endregion

    // region KaTypeParameterType modifications

    @Test
    @TestMetadata("typeParameterType/regularMakeNullable.kt")
    fun `typeParameterType regularMakeNullable +markNotNull`() = test<KaTypeParameterType> { type ->
        type.copy {
            isMarkedNullable = false
        }
    }

    @Test
    @TestMetadata("typeParameterType/reified.kt")
    fun `typeParameterType reified +markNullable`() = test<KaTypeParameterType> { type ->
        type.copy {
            isMarkedNullable = true
        }
    }

    @Test
    @TestMetadata("typeParameterType/withAnnotations.kt")
    fun `typeParameterType withAnnotations +addAnnotation`() = test<KaTypeParameterType> { type ->
        type.copy {
            annotation(ClassId.fromString("MyAnno4"))
        }
    }

    @Test
    @TestMetadata("typeParameterType/withAnnotations.kt")
    fun `typeParameterType withAnnotations +markNullable`() = test<KaTypeParameterType> { type ->
        type.copy {
            isMarkedNullable = true
        }
    }

    // endregion
}

/**
 * The base test checking type modification.
 * It reuses the existing infrastructure for DSL-based type creation tests ([AbstractTypeCreatorDslTest]).
 * Specifically, it creates the initial type using one of [DslTypeCreationTestCases], then it performs modification, and checks the
 * text representation of the resulting type.
 *
 * Tests should have the following naming: "<directoryName> <testDataFileName> +<modificationName>"
 * (modification component is separated with a plus sign), where `testDataFileName` is a base name of the test (without '.kt' or '.kts').
 */
abstract class AbstractTypeModificationDslTestBase(testDirPathString: String) : AbstractAnalysisApiExecutionTest(testDirPathString) {
    private companion object {
        private val TEST_NAME_REGEX = Regex("(\\w+) (\\w+) \\+(\\w+)")
    }

    @AnalysisApiTestEnvironmentStorage
    private lateinit var environment: AnalysisApiExecutionTestEnvironment

    protected fun <T : KaType> test(block: KaSession.(T) -> Any?) {
        val testMethodName = environment.testServices.testInfo.methodName
        val (directoryName, testDataFileName, modificationName) = TEST_NAME_REGEX.matchEntire(testMethodName)?.destructured
            ?: error("Expected test name format: 'directoryName testDataFileName +modificationName'")

        val mainFile = environment.mainFile ?: fail("Main file not found")
        val actualText = copyAwareAnalyzeForTest(mainFile) {
            TypeCreatorDslTestRenderer.createdTypePresentation(
                mainFile,
                environment.testServices,
                directoryName,
                testDataFileName
            ) { value ->
                requireNotNull(value)
                @Suppress("UNCHECKED_CAST")
                block(value as T)
            }
        }

        environment.testServices.assertions.assertEqualsToTestOutputFile(actualText, extension = ".$modificationName.txt")
    }
}
