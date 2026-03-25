/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.types

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation.AbstractTypeModificationDslTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.jupiter.api.Test
import kotlin.test.fail

@Suppress("JUnitTestCaseWithNoTests")
class FirIdeNormalAnalysisSourceModuleTypeModificationDslTest : AbstractTypeModificationDslTest() {
    override val configurator = LLSourceLikeTestConfigurator()
}

class UnusedTestOutputCheckingTest {
    @Test
    fun testTestDataUsage() {
        val modificationTest = FirIdeNormalAnalysisSourceModuleTypeModificationDslTest()
        val variantChain = modificationTest.variantChain
        val testDataPath = ForTestCompileRuntime.transformTestDataPath(modificationTest.testDirPathString)

        val existingOutputFiles = buildSet {
            for (file in testDataPath.walk()) {
                if (!file.isFile || file.extension != "txt") continue

                val fileNameChunks = file.nameWithoutExtension.split('.').takeIf { it.size >= 2 } ?: continue
                val modificationChunk = fileNameChunks.last()
                if (modificationChunk.isEmpty() || modificationChunk in variantChain) continue

                add(file)
            }
        }

        val coveredOutputFiles = buildSet {
            val testClasses = generateSequence<Class<*>>(modificationTest.javaClass) { it.superclass }
            for (testClass in testClasses) {
                for (testMethod in testClass.methods) {
                    if (!testMethod.isAnnotationPresent(Test::class.java)) continue

                    val methodName = testMethod.name
                    val (baseNameChunks, modificationChunk) = methodName.split('+', limit = 2).also { check(it.size == 2) }
                    val basePath = baseNameChunks.split(' ').map(String::trim).filter(String::isNotBlank).joinToString("/")
                    add(testDataPath.resolve("$basePath.$modificationChunk.txt"))
                }
            }
        }

        val uncoveredOutputFiles = (existingOutputFiles - coveredOutputFiles).sorted()
        if (uncoveredOutputFiles.isNotEmpty()) {
            val message = "The following test data outputs aren't used in tests:\n" + uncoveredOutputFiles.joinToString("\n")
            fail(message)
        }
    }
}
