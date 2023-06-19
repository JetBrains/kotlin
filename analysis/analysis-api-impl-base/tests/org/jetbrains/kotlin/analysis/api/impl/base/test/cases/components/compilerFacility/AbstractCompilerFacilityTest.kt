/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KtCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KtCompiledFile
import org.jetbrains.kotlin.analysis.api.components.KtCompilerFacility
import org.jetbrains.kotlin.analysis.api.components.KtCompilerTarget
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.codegen.BytecodeListingTextCollectingVisitor
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.File

abstract class AbstractCompilerFacilityTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val ktFile = ktFiles.singleOrNull() ?: ktFiles.first { it.name == "main.kt" }
        val ktCodeFragment = createCodeFragment(ktFile, module, testServices)

        if (ktCodeFragment != null) {
            for (importNameString in module.directives[Directives.CODE_FRAGMENT_IMPORT]) {
                ktCodeFragment.addImport("import $importNameString")
            }
        }

        val compilerConfiguration = CompilerConfiguration().apply {
            put(CommonConfigurationKeys.MODULE_NAME, module.name)
            put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, module.languageVersionSettings)
            put(JVMConfigurationKeys.IR, true)

            module.directives[Directives.CODE_FRAGMENT_CLASS_NAME].singleOrNull()
                ?.let { put(KtCompilerFacility.CODE_FRAGMENT_CLASS_NAME, it) }

            module.directives[Directives.CODE_FRAGMENT_METHOD_NAME].singleOrNull()
                ?.let { put(KtCompilerFacility.CODE_FRAGMENT_METHOD_NAME, it) }
        }

        val ktTargetFile = ktCodeFragment ?: ktFile

        analyze(ktTargetFile) {
            val target = KtCompilerTarget.Jvm(ClassBuilderFactories.TEST)
            val actualText = when (val result = compile(ktTargetFile, compilerConfiguration, target)) {
                is KtCompilationResult.Failure -> result.errors.joinToString("\n") { dumpDiagnostic(it) }
                is KtCompilationResult.Success -> dumpClassFiles(result.output)
            }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
        }
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
                +JvmEnvironmentConfigurationDirectives.FULL_JDK
            }
        }
    }

    private fun dumpDiagnostic(diagnostic: KtDiagnostic): String {
        val textRanges = when (diagnostic) {
            is KtDiagnosticWithPsi<*> -> {
                diagnostic.textRanges.singleOrNull()?.toString()
                    ?: diagnostic.textRanges.joinToString(prefix = "[", postfix = "]")
            }
            else -> null
        }

        return buildString {
            if (textRanges != null) {
                append(textRanges)
                append(" ")
            }
            append(diagnostic.factoryName)
            append(" ")
            append(diagnostic.defaultMessage)
        }
    }

    private fun dumpClassFiles(outputFiles: List<KtCompiledFile>): String {
        val classes = outputFiles
            .filter { it.path.endsWith(".class", ignoreCase = true) }
            .also { check(it.isNotEmpty()) }
            .sortedBy { it.path }
            .map { outputFile ->
                val classReader = ClassReader(outputFile.content)
                ClassNode(Opcodes.API_VERSION).also { classReader.accept(it, ClassReader.SKIP_CODE) }
            }

        val allClasses = classes.associateBy { Type.getObjectType(it.name) }

        return classes.joinToString("\n\n") { node ->
            val visitor = BytecodeListingTextCollectingVisitor(
                BytecodeListingTextCollectingVisitor.Filter.EMPTY,
                allClasses,
                withSignatures = false,
                withAnnotations = false,
                sortDeclarations = true
            )

            node.accept(visitor)
            visitor.text
        }
    }

    object Directives : SimpleDirectivesContainer() {
        val CODE_FRAGMENT_IMPORT by stringDirective("Import directive for a code fragment")
        val CODE_FRAGMENT_CLASS_NAME by stringDirective("Short name of a code fragment class")
        val CODE_FRAGMENT_METHOD_NAME by stringDirective("Name of a code fragment facade method")
    }
}

internal fun createCodeFragment(ktFile: KtFile, module: TestModule, testServices: TestServices): KtCodeFragment? {
    val ioFile = module.files.single { it.name == ktFile.name }.originalFile
    val ioFragmentFile = File(ioFile.parent, "${ioFile.nameWithoutExtension}.fragment.${ioFile.extension}")

    if (!ioFragmentFile.exists()) {
        return null
    }

    val contextElement = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)

    val fragmentText = ioFragmentFile.readText()
    val isBlockFragment = fragmentText.any { it == '\n' }

    val project = ktFile.project
    val factory = KtPsiFactory(project, markGenerated = false)

    return when {
        isBlockFragment -> factory.createBlockCodeFragment(fragmentText, contextElement)
        else -> factory.createExpressionCodeFragment(fragmentText, contextElement)
    }
}