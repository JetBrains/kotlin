/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.LLCompilerFacade
import org.jetbrains.kotlin.codegen.BytecodeListingTextCollectingVisitor
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes

abstract class AbstractCodeFragmentCompilationTest : AbstractLowLevelApiCodeFragmentTest() {
    override fun doTest(ktCodeFragment: KtCodeFragment, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val testModule = moduleStructure.modules.single()

        for (importNameString in moduleStructure.allDirectives[Directives.CODE_FRAGMENT_IMPORT]) {
            ktCodeFragment.addImport("import $importNameString")
        }

        val compilerConfiguration = CompilerConfiguration().apply {
            put(CommonConfigurationKeys.MODULE_NAME, testModule.name)
            put(JVMConfigurationKeys.IR, true)

            moduleStructure.allDirectives[Directives.CODE_FRAGMENT_CLASS_NAME].singleOrNull()
                ?.let { put(LLCompilerFacade.CODE_FRAGMENT_CLASS_NAME, it) }

            moduleStructure.allDirectives[Directives.CODE_FRAGMENT_METHOD_NAME].singleOrNull()
                ?.let { put(LLCompilerFacade.CODE_FRAGMENT_METHOD_NAME, it) }
        }

        val compilationResult = LLCompilerFacade
            .compile(ktCodeFragment, compilerConfiguration, testModule.languageVersionSettings, ClassBuilderFactories.TEST)
            .getOrThrow()

        compilationResult.outputFiles
            .filter { it.relativePath.toLowerCaseAsciiOnly().endsWith(".class") }
            .map { it.asText() }
            .forEach(::println)

        val actualText = if (compilationResult.diagnostics.isEmpty()) {
            compilationResult.outputFiles
                .filter { it.relativePath.toLowerCaseAsciiOnly().endsWith(".class") }
                .also { check(it.isNotEmpty()) }
                .joinToString("\n\n") { dumpClassFile(it.asByteArray()) }
        } else {
            compilationResult.diagnostics.joinToString("\n") { dumpDiagnostic(it) }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actualText)
    }

    private fun dumpDiagnostic(diagnostic: KtPsiDiagnostic): String {
        check(diagnostic is KtDiagnostic)

        val textRanges = diagnostic.textRanges
        val textRangesString = textRanges.singleOrNull()?.toString()
            ?: textRanges.joinToString(prefix = "[", postfix = "]")

        val factory = RootDiagnosticRendererFactory(diagnostic)
        return buildString {
            append(textRangesString)
            append(" ")
            append(diagnostic.factoryName)
            append(" ")
            append(factory.render(diagnostic))
        }
    }

    private fun dumpClassFile(bytes: ByteArray): String {
        val visitor = BytecodeListingTextCollectingVisitor(
            BytecodeListingTextCollectingVisitor.Filter.EMPTY,
            withSignatures = false,
            api = Opcodes.API_VERSION,
            withAnnotations = false,
            sortDeclarations = true
        )

        ClassReader(bytes).accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG)
        return visitor.text
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

    object Directives : SimpleDirectivesContainer() {
        val CODE_FRAGMENT_IMPORT by stringDirective("Import directive for a code fragment")
        val CODE_FRAGMENT_CLASS_NAME by stringDirective("Short name of a code fragment class")
        val CODE_FRAGMENT_METHOD_NAME by stringDirective("Name of a code fragment facade method")
    }
}