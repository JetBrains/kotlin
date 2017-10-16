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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.`when`.WhenByEnumsMapping.MAPPINGS_CLASS_NAME_POSTFIX
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.KotlinTestUtils.getAnnotationsJar
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.io.File

abstract class AbstractLightAnalysisModeTest : CodegenTestCase() {
    private companion object {
        var TEST_LIGHT_ANALYSIS: ClassBuilderFactory = object : ClassBuilderFactories.TestClassBuilderFactory(false) {
            override fun getClassBuilderMode() = ClassBuilderMode.LIGHT_ANALYSIS_FOR_TESTS
        }
    }

    override fun doMultiFileTest(wholeFile: File, files: List<CodegenTestCase.TestFile>, javaFilesDir: File?) {
        for (file in files) {
            if (file.content.contains("// IGNORE_LIGHT_ANALYSIS")) {
                return
            }
        }

        val fullTxt = compileWithFullAnalysis(files, javaFilesDir)
                .replace("final enum class", "enum class")

        val liteTxt = compileWithLightAnalysis(wholeFile, files, javaFilesDir)
                .replace("@synthetic.kotlin.jvm.GeneratedByJvmOverloads ", "")

        assertEquals(fullTxt, liteTxt)
    }

    override fun verifyWithDex(): Boolean {
        return false
    }

    private fun compileWithLightAnalysis(wholeFile: File, files: List<CodegenTestCase.TestFile>, javaFilesDir: File?): String {
        val boxTestsDir = File("compiler/testData/codegen/box")
        val relativePath = wholeFile.toRelativeString(boxTestsDir)
        // Fail if this test is not under codegen/box
        assert(!relativePath.startsWith(".."))

        val configuration = createConfiguration(
                configurationKind, getJdkKind(files), listOf(getAnnotationsJar()), javaFilesDir?.let(::listOf).orEmpty(), files
        )
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        AnalysisHandlerExtension.registerExtension(environment.project, PartialAnalysisHandlerExtension())

        val testFiles = loadMultiFiles(files, environment.project)
        val classFileFactory = GenerationUtils.compileFiles(testFiles.psiFiles, environment, TEST_LIGHT_ANALYSIS).factory

        return BytecodeListingTextCollectingVisitor.getText(classFileFactory, ListAnalysisFilter(), replaceHash = false)
    }

    protected fun compileWithFullAnalysis(
            files: List<TestFile>,
            javaSourceDir: File?
    ): String {
        compile(files, javaSourceDir)
        classFileFactory.getClassFiles()

        val classInternalNames = classFileFactory.generationState.bindingContext
                .getSliceContents(CodegenBinding.ASM_TYPE).map { it.value.internalName to it.key }.toMap()

        return BytecodeListingTextCollectingVisitor.getText(classFileFactory, object : ListAnalysisFilter() {
            override fun shouldWriteClass(access: Int, name: String): Boolean {
                val classDescriptor = classInternalNames[name]
                if (classDescriptor != null && shouldFilterClass(classDescriptor)) {
                    return false
                }
                return super.shouldWriteClass(access, name)
            }

            override fun shouldWriteInnerClass(name: String): Boolean {
                val classDescriptor = classInternalNames[name]
                if (classDescriptor != null && shouldFilterClass(classDescriptor)) {
                    return false
                }
                return super.shouldWriteInnerClass(name)
            }

            private fun shouldFilterClass(descriptor: ClassDescriptor): Boolean {
                return descriptor.visibility == Visibilities.LOCAL || descriptor is SyntheticClassDescriptorForLambda
            }
        }, replaceHash = false)
    }

    private open class ListAnalysisFilter : BytecodeListingTextCollectingVisitor.Filter {
        override fun shouldWriteClass(access: Int, name: String) = when {
            name.endsWith(MAPPINGS_CLASS_NAME_POSTFIX) && (access and ACC_SYNTHETIC != 0) && (access and ACC_FINAL != 0) -> false
            name.contains("\$\$inlined") && (access and ACC_FINAL != 0) -> false
            name.contains("\$sam\$") -> false
            else -> true
        }

        override fun shouldWriteMethod(access: Int, name: String, desc: String) = when {
            name == "<clinit>" -> false
            AsmTypes.DEFAULT_CONSTRUCTOR_MARKER.descriptor in desc -> false
            name.startsWith("access$") && (access and ACC_STATIC != 0) && (access and ACC_SYNTHETIC != 0) -> false
            else -> true
        }

        override fun shouldWriteField(access: Int, name: String, desc: String) = when {
            name == "\$VALUES" && (access and ACC_PRIVATE != 0) && (access and ACC_FINAL != 0) && (access and ACC_SYNTHETIC != 0) -> false
            name == JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME && (access and ACC_SYNTHETIC != 0) -> false
            else -> true
        }

        override fun shouldWriteInnerClass(name: String) = true
    }
}
