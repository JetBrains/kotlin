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
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractCompilerLightClassTest : KotlinMultiFileTestWithJava<Void, Void>() {
    override fun getConfigurationKind(): ConfigurationKind = ConfigurationKind.ALL

    override fun isKotlinSourceRootNeeded(): Boolean = true

    override fun doMultiFileTest(file: File, modules: MutableMap<String, ModuleAndDependencies>, files: MutableList<Void>) {
        val environment = createEnvironment(file)
        val expectedFile = KotlinTestUtils.replaceExtension(file, "java")
        LightClassTestCommon.testLightClass(
                expectedFile,
                file,
                { fqname -> findLightClass(environment, fqname) },
                LightClassTestCommon::removeEmptyDefaultImpls
        )
    }

    override fun createTestModule(name: String): Void? = null

    override fun createTestFile(module: Void?, fileName: String, text: String, directives: Map<String, String>): Void? = null

    companion object {
        fun findLightClass(environment: KotlinCoreEnvironment, fqname: String): PsiClass? {
            KotlinTestUtils.resolveAllKotlinFiles(environment)

            val lightCLassForScript = LightClassGenerationSupport
                    .getInstance(environment.project)
                    .getScriptClasses(FqName(fqname), GlobalSearchScope.allScope(environment.project))
                    .firstOrNull()

            return lightCLassForScript ?: JavaElementFinder
                    .getInstance(environment.project)
                    .findClass(fqname, GlobalSearchScope.allScope(environment.project))
        }
    }
}
