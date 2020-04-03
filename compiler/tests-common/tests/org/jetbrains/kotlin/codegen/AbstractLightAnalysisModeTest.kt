/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
        var TEST_LIGHT_ANALYSIS: ClassBuilderFactory = object : ClassBuilderFactories.TestClassBuilderFactory() {
            override fun getClassBuilderMode() = ClassBuilderMode.getLightAnalysisForTests()
        }
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        for (file in files) {
            if (file.content.contains("// IGNORE_LIGHT_ANALYSIS")) {
                return
            }
        }

        val fullTxt = compileWithFullAnalysis(files)
            .replace("final enum class", "enum class")

        val liteTxt = compileWithLightAnalysis(wholeFile, files)
            .replace("@synthetic.kotlin.jvm.GeneratedByJvmOverloads ", "")

        assertEquals(fullTxt, liteTxt)
    }

    override fun verifyWithDex(): Boolean {
        return false
    }

    private fun compileWithLightAnalysis(wholeFile: File, files: List<TestFile>): String {
        val boxTestsDir = File("compiler/testData/codegen/box")
        val relativePath = wholeFile.toRelativeString(boxTestsDir)
        // Fail if this test is not under codegen/box
        assert(!relativePath.startsWith(".."))

        val configuration = createConfiguration(
            configurationKind, getTestJdkKind(files), listOf(getAnnotationsJar()), listOfNotNull(writeJavaFiles(files)), files
        )
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        AnalysisHandlerExtension.registerExtension(environment.project, PartialAnalysisHandlerExtension())

        val testFiles = loadMultiFiles(files, environment.project)
        val classFileFactory = GenerationUtils.compileFiles(testFiles.psiFiles, environment, TEST_LIGHT_ANALYSIS).factory

        return BytecodeListingTextCollectingVisitor.getText(classFileFactory, ListAnalysisFilter())
    }

    private fun compileWithFullAnalysis(files: List<TestFile>): String {
        compile(files)
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
        })
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
            name.contains("\$\$forInline") -> false
            AsmTypes.DEFAULT_CONSTRUCTOR_MARKER.descriptor in desc -> false
            name.startsWith("access$") && (access and ACC_STATIC != 0) && (access and ACC_SYNTHETIC != 0) -> false
            else -> true
        }

        override fun shouldWriteField(access: Int, name: String, desc: String) = when {
            name == "\$assertionsDisabled" -> false
            name == "\$VALUES" && (access and ACC_PRIVATE != 0) && (access and ACC_FINAL != 0) && (access and ACC_SYNTHETIC != 0) -> false
            name == JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME && (access and ACC_SYNTHETIC != 0) -> false
            else -> true
        }

        override fun shouldWriteInnerClass(name: String) = true
    }
}
