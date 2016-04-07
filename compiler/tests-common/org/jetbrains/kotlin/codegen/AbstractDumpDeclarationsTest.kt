/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractDumpDeclarationsTest : CodegenTestCase() {

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val expectedResult = KotlinTestUtils.replaceExtension(wholeFile, "json")
        compileAndCompareDump(files, expectedResult)
    }


    private fun compileAndCompareDump(files: List<TestFile>, expectedResult: File) {
        configurationKind = ConfigurationKind.NO_KOTLIN_REFLECT

        val configuration = KotlinTestUtils.compilerConfigurationForTests(
                configurationKind, TestJdkKind.MOCK_JDK, listOf(KotlinTestUtils.getAnnotationsJar()), emptyList())

        myEnvironment = KotlinCoreEnvironment.createForTests(
                testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        loadMultiFiles(files)

        val declarationsFile = compileManyFilesGetDeclarationsDump(myFiles.psiFiles)
        KotlinTestUtils.assertEqualsToFile(expectedResult, declarationsFile.readText())
    }

    private fun compileManyFilesGetDeclarationsDump(files: List<KtFile>): File {
        val project = myEnvironment.project
        val packagePartProvider = JvmPackagePartProvider(myEnvironment)

        val analysisResult = JvmResolveUtil.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                project, files, packagePartProvider)

        analysisResult.throwIfError()

        val dumpToFile = KotlinTestUtils.tmpDirForTest(this).resolve(this.name + ".json")

        val state = GenerationState(
                project, ClassBuilderFactories.TEST,
                analysisResult.moduleDescriptor, analysisResult.bindingContext,
                files,
                disableCallAssertions = false,
                disableParamAssertions = false,
                dumpBinarySignatureMappingTo = dumpToFile)
        KotlinCodegenFacade.compileCorrectFiles(state, org.jetbrains.kotlin.codegen.CompilationErrorHandler.THROW_EXCEPTION)

        state.destroy()

        return dumpToFile
    }
}
