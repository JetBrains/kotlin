/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.util.KotlinFrontEndException
import org.junit.Assert
import java.io.File

abstract class AbstractCompilerLightClassTest : KotlinMultiFileTestWithJava<KotlinBaseTest.TestModule, KotlinBaseTest.TestFile>() {

    override fun isKotlinSourceRootNeeded(): Boolean = true

    override fun doMultiFileTest(file: File, files: List<TestFile>) {
        val environment = createEnvironment(file, files)
        val expectedFile = KotlinTestUtils.replaceExtension(file, "java")
        val allowFrontendExceptions = InTextDirectivesUtils.isDirectiveDefined(file.readText(), "// ALLOW_FRONTEND_EXCEPTION")

        LightClassTestCommon.testLightClass(
            expectedFile,
            file,
            { fqname -> findLightClass(allowFrontendExceptions, environment, fqname) },
            LightClassTestCommon::removeEmptyDefaultImpls
        )
    }

    override fun createTestModule(
        name: String,
        dependencies: List<String>,
        friends: List<String>
    ): TestModule? = null

    override fun createTestFile(
        module: TestModule?,
        fileName: String,
        text: String,
        directives: Directives
    ): TestFile = TestFile(fileName, text, directives)

    companion object {
        fun findLightClass(allowFrontendExceptions: Boolean, environment: KotlinCoreEnvironment, fqname: String): PsiClass? {
            assertException(allowFrontendExceptions, KotlinFrontEndException::class.java) {
                KotlinTestUtils.resolveAllKotlinFiles(environment)
            }

            val lightCLassForScript = KotlinAsJavaSupport
                .getInstance(environment.project)
                .getScriptClasses(FqName(fqname), GlobalSearchScope.allScope(environment.project))
                .firstOrNull()

            return lightCLassForScript ?: JavaElementFinder
                .getInstance(environment.project)
                .findClass(fqname, GlobalSearchScope.allScope(environment.project))
        }

        private fun assertException(shouldOccur: Boolean, klass: Class<out Throwable>, f: () -> Unit) {
            if (!shouldOccur) {
                f()
                return
            }

            var wasThrown = false
            try {
                f()
            } catch (e: Throwable) {
                wasThrown = true

                if (!shouldOccur || !klass.isAssignableFrom(e.javaClass)) {
                    throw e
                }
            } finally {
                if (shouldOccur && !wasThrown) {
                    Assert.fail("Expected exception wasn't thrown")
                }
            }
        }
    }
}
