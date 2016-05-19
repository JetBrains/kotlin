/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.typeApproximation

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.inference.createCapturedType
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypesIfNecessary
import java.io.File
import java.util.*

class CapturedTypeApproximationTest : KotlinTestWithEnvironment() {
    private val testDataPath: String
        get() = KotlinTestUtils.getTestDataPathBase() + "/capturedTypeApproximation/"

    override fun createEnvironment(): KotlinCoreEnvironment = createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY)

    fun doTest(filePath: String, vararg substitutions: String) {
        assert(substitutions.size in 1..2) { "Captured type approximation test requires substitutions for (T) or (T, R)" }
        val oneTypeVariable = substitutions.size == 1

        val declarationsText = KotlinTestUtils.doLoadFile(File(testDataPath + "/declarations.kt"))

        fun analyzeTestFile(testType: String) = run {
            val test = declarationsText.replace("#TestType#", testType)
            val testFile = KtPsiFactory(project).createFile(test)
            val bindingContext = JvmResolveUtil.analyze(testFile, environment).bindingContext
            val functions = bindingContext.getSliceContents(BindingContext.FUNCTION)
            val functionFoo = functions.values.firstOrNull { it.name.asString() == "foo" } ?:
                              throw AssertionError("Function 'foo' is not declared")
            Pair(bindingContext, functionFoo)
        }

        fun createTestType(testTypeWithT: String): String {
            val testType = testTypeWithT.replace("#T#", substitutions[0])
            if (oneTypeVariable) return testType
            return testType.replace("#R#", substitutions[1])
        }

        fun createTestSubstitutions(typeParameters: List<TypeParameterDescriptor>) = run {
            val builtIns = DefaultBuiltIns.Instance
            val intType = builtIns.intType
            val stringType = builtIns.stringType
            val t = typeParameters[0]
            val r = typeParameters[1]
            if (oneTypeVariable)
                listOf(mapOf(t to TypeProjectionImpl(IN_VARIANCE, intType)),
                       mapOf(t to TypeProjectionImpl(OUT_VARIANCE, intType)))
            else {
                listOf(mapOf(
                        t to TypeProjectionImpl(IN_VARIANCE, intType),
                        r to TypeProjectionImpl(OUT_VARIANCE, stringType))
                )
            }
        }

        fun createTestSubstitutor(testSubstitution: Map<TypeParameterDescriptor, TypeProjection>): TypeSubstitutor {
            val substitutionContext = testSubstitution.map {
                val (typeParameter, typeProjection) = it
                typeParameter.typeConstructor to TypeProjectionImpl(createCapturedType(typeProjection))
            }.toMap()
            return TypeSubstitutor.create(substitutionContext)
        }

        val testTypes = if (oneTypeVariable) getTestTypesForOneTypeVariable() else getTestTypesForTwoTypeVariables()
        val result = buildString {
            for ((index, testTypeWithUnsubstitutedTypeVars) in testTypes.withIndex()) {
                val testType = createTestType(testTypeWithUnsubstitutedTypeVars)
                val (bindingContext, functionFoo) = analyzeTestFile(testType)
                val typeParameters = functionFoo.typeParameters
                val type = functionFoo.returnType

                appendln(testType)

                if (bindingContext.diagnostics.noSuppression().any { it.severity == Severity.ERROR }) {
                    appendln("  compiler error\n")
                    continue
                }

                val testSubstitutions = createTestSubstitutions(typeParameters)
                for (testSubstitution in testSubstitutions) {
                    val typeSubstitutor = createTestSubstitutor(testSubstitution)
                    val typeWithCapturedType = typeSubstitutor.substituteWithoutApproximation(TypeProjectionImpl(INVARIANT, type!!))!!.type

                    val (lower, upper) = approximateCapturedTypes(typeWithCapturedType)
                    val substitution =
                            approximateCapturedTypesIfNecessary(
                                    TypeProjectionImpl(INVARIANT, typeWithCapturedType), approximateContravariant = false)

                    append("  ")
                    for (typeParameter in testSubstitution.keys) {
                        if (testSubstitution.size > 1) append("${typeParameter.name} = ")
                        append("${testSubstitution[typeParameter]}. ")
                    }
                    appendln("lower: $lower; upper: $upper; substitution: $substitution")
                }
                if (testTypes.lastIndex != index) appendln()
            }
        }

        KotlinTestUtils.assertEqualsToFile(File(testDataPath + "/" + filePath), result)
    }

    private fun getTypePatternsForOneTypeVariable() = listOf("In<#T#>", "Out<#T#>", "Inv<#T#>", "Inv<in #T#>", "Inv<out #T#>")
    private fun getTypePatternsForTwoTypeVariables() = listOf("Fun<#T#, #R#>", "Inv2<#T#, #R#>")

    private fun getTestTypesForOneTypeVariable(): List<String> {
        val typePatterns = getTypePatternsForOneTypeVariable()

        val range = typePatterns.indices
        val variants = ArrayList<List<Int>>()
        for (i in range) variants.add(listOf(i))
        for (i in range) for (j in range) variants.add(listOf(i, j))

        fun addRandomVariants(vararg randomVariants: String) {
            variants.addAll(randomVariants.map { digits -> digits.map { digit -> digit - '0' } })
        }
        assert(typePatterns.size == 5) { "Generated random variants below depend on size 5" }
        //From 021 the following is generated: In<Inv<Out<T>>>, where In = typePatterns[0], Inv = typePatterns[2], Out = typePatterns[1]
        addRandomVariants("021", "111", "230", "421", "322", "120", "411", "102", "401", "012")
        addRandomVariants("4243", "3103", "3043", "2003", "4442", "4143", "1440", "0303", "1302", "1332")
        addRandomVariants("00200", "22213", "12114", "20304", "34014", "41333", "11214", "02004", "43244", "03004")
        addRandomVariants("021022", "124230", "210030", "202344", "043234", "024400", "102121", "423143", "132121", "233001")

        return variants.map { it.fold("#T#") { type, index -> type.replace("#T#", typePatterns[index]) } }
    }

    private fun getTestTypesForTwoTypeVariables(): List<String> {
        val typePatterns = getTypePatternsForOneTypeVariable()

        val range = typePatterns.indices
        val result = ArrayList<String>()
        for (pattern in getTypePatternsForTwoTypeVariables()) {
            for (i in range) {
                result.add(typePatterns[i].replace("#T#", pattern))
            }
            for (i in range) {
                for (j in range) {
                    result.add(pattern.replace("#T#", typePatterns[i]).replace("#R#", typePatterns[j].replace("#T#", "#R#")))
                }
            }
        }
        return result
    }

    fun testSimpleT() {
        doTest("simpleT.txt", "T")
    }

    fun testNullableT() {
        doTest("nullableT.txt", "T?")
    }

    fun testUseSiteInT() {
        doTest("useSiteInT.txt", "in T")
    }

    fun testUseSiteInNullableT() {
        doTest("useSiteInNullableT.txt", "in T?")
    }

    fun testUseSiteOutT() {
        doTest("useSiteOutT.txt", "out T")
    }

    fun testUseSiteOutNullableT() {
        doTest("useSiteOutNullableT.txt", "out T?")
    }

    fun testTwoVariables() {
        doTest("twoVariables.txt", "T", "R")
    }
}
