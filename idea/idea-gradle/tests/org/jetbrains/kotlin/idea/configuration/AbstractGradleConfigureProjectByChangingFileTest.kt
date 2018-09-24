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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.isAllFilesPresentTest
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import java.io.File

abstract class AbstractGradleConfigureProjectByChangingFileTest :
    AbstractConfigureProjectByChangingFileTest<KotlinWithGradleConfigurator>() {
    fun doTestGradle(path: String) {
        val (before, after) = beforeAfterFiles()
        doTest(before, after, if ("js" in path) KotlinJsGradleModuleConfigurator() else KotlinGradleModuleConfigurator())
    }

    private fun beforeAfterFiles(): Pair<String, String> {
        val root = KotlinTestUtils.getTestsRoot(this::class.java)
        val test = KotlinTestUtils.getTestDataFileName(this::class.java, name)
        val path = "$root/$test"
        val testFile = File(path)

        if (testFile.isFile) {
            return path to path.replace("before", "after")
        }

        return when {
            File(path, "build_before.gradle").exists() ->
                "$path/build_before.gradle" to "$path/build_after.gradle"

            File(path, "build_before.gradle.kts").exists() ->
                "$path/build_before.gradle.kts" to "$path/build_after.gradle.kts"

            else -> error("Can't find test data files")
        }
    }

    override fun runConfigurator(
        module: Module,
        file: PsiFile,
        configurator: KotlinWithGradleConfigurator,
        version: String,
        collector: NotificationMessageCollector
    ) {
        if (file !is GroovyFile && file !is KtFile) {
            fail("file $file is not a GroovyFile or KtFile")
            return
        }

        configurator.configureModule(module, file, true, version, collector, ArrayList())
        configurator.configureModule(module, file, false, version, collector, ArrayList())
    }

    override fun getProjectJDK(): Sdk {
        if (!isAllFilesPresentTest(getTestName(false))) {
            val beforeAfterFiles = beforeAfterFiles()
            val (before, _) = beforeAfterFiles
            val gradleFile = File(before)
            if (gradleFile.readText().contains("1.9")) {
                return PluginTestCaseBase.mockJdk9()
            }
        }

        return super.getProjectJDK()
    }
}
