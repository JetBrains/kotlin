/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.components.KaCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KaCompilerTarget
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.TestAllowedErrorFilter
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.dumpClassFiles
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.AbstractDeclarationTypeAsPsiTypeTest.Directives.RENDER_CLASS_DUMP
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.AbstractDeclarationTypeAsPsiTypeTest.Directives.TYPE_MAPPING_MODE
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.findLightDeclarationContext
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.getContainingKtLightClass
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent.JavaInteroperabilityComponentTestUtils.render
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtDeclarationWithReturnType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractDeclarationTypeAsPsiTypeTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<KtDeclarationWithReturnType>(mainFile)
        val psiContext = if (KtPsiUtil.isLocal(declaration)) {
            declaration
        } else {
            val containingClass = getContainingKtLightClass(declaration, mainFile)
            containingClass.findLightDeclarationContext(declaration) ?: error("Can't find psi context for $declaration")
        }

        val actual = buildString {
            executeOnPooledThreadInReadAction {
                copyAwareAnalyzeForTest(declaration) { contextDeclaration ->
                    val kaType = contextDeclaration.returnType
                    val mode = testServices.moduleStructure.allDirectives.singleOrZeroValue(TYPE_MAPPING_MODE) ?: KaTypeMappingMode.DEFAULT
                    val psiType = kaType.asPsiType(psiContext, allowErrorTypes = false, mode = mode)

                    appendLine("${KaType::class.simpleName}: ${kaType.render(useSiteSession)}")
                    appendLine("${PsiType::class.simpleName}: ${psiType?.render()}")

                    if (mainModule.testModule.directives.contains(RENDER_CLASS_DUMP)) {
                        appendLine()
                        appendLine("Compilation result:")

                        val compilationResult = compile(
                            mainFile,
                            configuration = CompilerConfiguration().apply {
                                put(CommonConfigurationKeys.MODULE_NAME, mainModule.testModule.name)
                                put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, mainModule.testModule.languageVersionSettings)
                            },
                            target = KaCompilerTarget.Jvm(isTestMode = true, compiledClassHandler = null, debuggerExtension = null),
                            allowedErrorFilter = TestAllowedErrorFilter
                        )

                        when (compilationResult) {
                            is KaCompilationResult.Failure -> {
                                for (diagnostic in compilationResult.errors) {
                                    appendLine(stringRepresentation(diagnostic))
                                }
                            }
                            is KaCompilationResult.Success -> {
                                val classFileDump = dumpClassFiles(
                                    compilationResult.output,
                                    dumpSignatures = true,
                                    dumpAnnotations = true,
                                    dumpCode = false,
                                )

                                appendLine(classFileDump)
                            }
                        }
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    private object Directives : SimpleDirectivesContainer() {
        val RENDER_CLASS_DUMP by directive(
            "Render compiled class dump for comparison with the Analysis API output"
        )

        val TYPE_MAPPING_MODE by enumDirective<KaTypeMappingMode>(
            description = "Custom type mapping mode",
            applicability = DirectiveApplicability.Global
        )
    }
}
