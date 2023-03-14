/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.util.KtTestUtil.getAnnotationsJar
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.File

abstract class AbstractLightAnalysisModeTest : CodegenTestCase() {
    private companion object {
        var TEST_LIGHT_ANALYSIS: ClassBuilderFactory = object : ClassBuilderFactories.TestClassBuilderFactory() {
            override fun getClassBuilderMode() = ClassBuilderMode.getLightAnalysisForTests()
        }

        private val ignoreDirectives = listOf(
            "// IGNORE_LIGHT_ANALYSIS",
            "// MODULE:",
            "// TARGET_FRONTEND: FIR",
            "// IGNORE_BACKEND_K1:",
        )

        // current ignore+unmute logic doesn't support the situation when LA successds but codegen fails
        // so this is a very dirty hack to support it for one particular case - signedToUnsignedConversions.kt
        // We assume that these tests will soo be irrelevant and therefore the hack will die too
        private val failDirective = "FAIL_IN_LIGHT_ANALYSIS"
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        var isIgnored = false
        for (file in files) {
            if (file.content.contains(failDirective)) throw RuntimeException("Forced ignore for this test")
            if (!isIgnored && ignoreDirectives.any { file.content.contains(it) }) isIgnored = true
        }
        if (isIgnored) return

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
            configurationKind, getTestJdkKind(files), backend, listOf(getAnnotationsJar()), listOfNotNull(writeJavaFiles(files)), files
        )
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        AnalysisHandlerExtension.registerExtension(environment.project, PartialAnalysisHandlerExtension())

        val testFiles = loadMultiFiles(files, environment.project)
        val classFileFactory = GenerationUtils.compileFiles(testFiles.psiFiles, environment, TEST_LIGHT_ANALYSIS).factory

        return BytecodeListingTextCollectingVisitor.getText(classFileFactory, ListAnalysisFilter())
    }

    private fun compileWithFullAnalysis(files: List<TestFile>): String {
        compile(files)
        return BytecodeListingTextCollectingVisitor.getText(classFileFactory, ListAnalysisFilter())
    }

    private class ListAnalysisFilter : BytecodeListingTextCollectingVisitor.Filter {
        @Suppress("UNCHECKED_CAST")
        override fun shouldWriteClass(node: ClassNode): Boolean {
            val metadata = node.visibleAnnotations.singleOrNull { it.desc == "Lkotlin/Metadata;" }
                ?: error("No kotlin.Metadata generated for class ${node.name}")
            val args = metadata.values.chunked(2).associate { (x, y) -> x to y }
            val kind = args["k"] as Int
            return when (Kind.getById(kind)) {
                Kind.UNKNOWN -> error(node.name)
                Kind.CLASS -> {
                    val d1 = (args["d1"] as List<String>).toTypedArray()
                    val d2 = (args["d2"] as List<String>).toTypedArray()
                    val (_, proto) = JvmProtoBufUtil.readClassDataFrom(d1, d2)
                    Flags.VISIBILITY.get(proto.flags) != ProtoBuf.Visibility.LOCAL
                }
                Kind.FILE_FACADE, Kind.MULTIFILE_CLASS, Kind.MULTIFILE_CLASS_PART -> true
                Kind.SYNTHETIC_CLASS -> false
            }
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

        override fun shouldWriteInnerClass(name: String, outerName: String?, innerName: String?) =
            outerName != null && innerName != null
    }
}
