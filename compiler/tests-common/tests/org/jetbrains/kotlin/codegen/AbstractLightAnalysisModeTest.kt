/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.ReplaceWithSupertypeAnonymousTypeTransformer
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.TargetBackend
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
    }

    override val backend: TargetBackend
        get() = TargetBackend.JVM_IR

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        for (file in files) {
            if (ignoreDirectives.any { file.content.contains(it) }) return
        }

        val fullTxt = compileWithFullAnalysis(files)

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

        configurationKind = extractConfigurationKind(files)
        val configuration = createConfiguration(
            configurationKind, getTestJdkKind(files), backend, listOf(getAnnotationsJar()), listOfNotNull(writeJavaFiles(files)), files
        )
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        AnalysisHandlerExtension.registerExtension(environment.project, PartialAnalysisHandlerExtension())
        StorageComponentContainerContributor.registerExtension(
            environment.project,
            object : StorageComponentContainerContributor {
                override fun registerModuleComponents(
                    container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor
                ) {
                    container.useInstance(ReplaceWithSupertypeAnonymousTypeTransformer())
                }
            }
        )

        val testFiles = loadMultiFiles(files, environment.project)
        val classFileFactory = GenerationUtils.compileFiles(testFiles.psiFiles, environment, TEST_LIGHT_ANALYSIS).factory

        return BytecodeListingTextCollectingVisitor.getText(classFileFactory, ListAnalysisFilter())
    }

    private fun compileWithFullAnalysis(files: List<TestFile>): String {
        compile(files)
        return BytecodeListingTextCollectingVisitor.getText(classFileFactory, ListAnalysisFilter())
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        configureIrAnalysisFlag(configuration)
    }

    // TODO: rewrite the test on the new infrastructure, so that this won't be needed.
    private fun configureIrAnalysisFlag(configuration: CompilerConfiguration) {
        val irFlag: Map<AnalysisFlag<*>, Boolean> = mapOf(JvmAnalysisFlags.useIR to backend.isIR)
        val lvs = configuration.languageVersionSettings
        if (lvs is CompilerTestLanguageVersionSettings) {
            configuration.languageVersionSettings = LanguageVersionSettingsImpl(
                lvs.languageVersion, lvs.apiVersion, lvs.analysisFlags + irFlag, lvs.extraLanguageFeatures,
            )
        } else {
            configuration.languageVersionSettings = LanguageVersionSettingsImpl(
                LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE, irFlag,
            )
        }
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
            access and ACC_SYNTHETIC != 0 -> false
            access and ACC_PRIVATE != 0 -> false
            name == "<clinit>" -> false
            name.contains("\$\$forInline") -> false
            AsmTypes.DEFAULT_CONSTRUCTOR_MARKER.descriptor in desc -> false
            name.startsWith("access$") && (access and ACC_STATIC != 0) -> false
            else -> true
        }

        override fun shouldWriteField(access: Int, name: String, desc: String) = when {
            name == "\$assertionsDisabled" -> false
            name == "\$VALUES" && (access and ACC_PRIVATE != 0) && (access and ACC_FINAL != 0) && (access and ACC_SYNTHETIC != 0) -> false
            name == JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME && (access and ACC_SYNTHETIC != 0) -> false
            name.endsWith("\$receiver") -> false
            JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX in name -> false
            else -> true
        }

        // Generated InnerClasses attributes depend on which types are used in method bodies, so they can easily be non-equal
        // among full and light analysis modes.
        override fun shouldWriteInnerClass(name: String, outerName: String?, innerName: String?, access: Int): Boolean =
            false

        override val shouldTransformAnonymousTypes: Boolean
            get() = true
    }
}
