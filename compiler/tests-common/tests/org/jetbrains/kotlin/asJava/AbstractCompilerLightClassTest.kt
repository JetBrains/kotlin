/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinBaseTest
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.util.KotlinFrontEndException
import org.junit.Assert
import java.io.File

abstract class AbstractCompilerLightClassTest : KotlinMultiFileTestWithJava<KotlinBaseTest.TestModule, KotlinBaseTest.TestFile>() {

    override fun isKotlinSourceRootNeeded(): Boolean = true

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        val environment = createEnvironment(wholeFile, files)
        val expectedFile = KotlinTestUtils.replaceExtension(wholeFile, "descriptors.java").takeIf(File::exists)
            ?: KotlinTestUtils.replaceExtension(wholeFile, "java")

        val allowFrontendExceptions = InTextDirectivesUtils.isDirectiveDefined(wholeFile.readText(), "// ALLOW_FRONTEND_EXCEPTION")

        val actual = LightClassTestCommon.getActualLightClassText(
            wholeFile,
            { fqname -> findLightClass(allowFrontendExceptions, environment, fqname) },
            { LightClassTestCommon.removeEmptyDefaultImpls(it).replace("\$test_module", "\$light_idea_test_case") },
        )
        KotlinTestUtils.assertEqualsToFile(expectedFile, actual)
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

            val searchScope = GlobalSearchScope.allScope(environment.project)
            val kotlinAsJavaSupport = KotlinAsJavaSupport.getInstance(environment.project)
            val lightClassForScript = kotlinAsJavaSupport
                .getScriptClasses(FqName(fqname), searchScope)
                .firstOrNull()

            return lightClassForScript
                ?: JavaElementFinder.getInstance(environment.project).findClass(fqname, searchScope)
                ?: kotlinAsJavaSupport.findClassOrObjectDeclarations(FqName(fqname), searchScope).firstOrNull {
                    it is KtEnumEntry // JavaElementFinder will ignore enum entry
                }?.toLightClass()
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
