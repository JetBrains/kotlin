/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirLazyDeclarationResolveTestCase.Directives.LAZY_MODE
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.*
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.NO_RUNTIME
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractFirLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveOverAllPhasesTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    private object Directives : SimpleDirectivesContainer() {
        val DANGLING_FILE_RESOLUTION_MODE by enumDirective(description = "Dangling file resolution mode for a copy") {
            KaDanglingFileResolutionMode.valueOf(it)
        }

        val DANGLING_FILE_CUSTOM_PACKAGE by stringDirective(
            description = "Custom package for a dangling file. Note: <caret> search won't work with a new package",
        )
    }

    override fun checkResolutionFacade(resolutionFacade: LLResolutionFacade) {
        require(resolutionFacade.isSourceSession)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val danglingFileResolutionMode = testServices.moduleStructure
            .allDirectives
            .singleOrZeroValue(Directives.DANGLING_FILE_RESOLUTION_MODE)

        val fileToTest = if (danglingFileResolutionMode == null) {
            mainFile
        } else {
            val customPackage = testServices.moduleStructure.allDirectives.singleOrZeroValue(Directives.DANGLING_FILE_CUSTOM_PACKAGE)
            val newText = mainFile.text.let { oldText ->
                if (customPackage == null) {
                    oldText
                } else {
                    val newPackage = "package $customPackage"
                    val oldPackage = mainFile.packageDirective?.text
                    if (oldPackage.isNullOrBlank()) {
                        // Note: a new package won't work in a combination of <caret> search
                        // due to `PsiTreeUtil.findSameElementInCopy` implementation
                        buildString {
                            appendLine(newPackage)
                            appendLine()
                            append(oldText)
                        }
                    } else {
                        oldText.replaceFirst(oldPackage, newPackage)
                    }
                }
            }

            KtPsiFactory.contextual(mainFile).createFile("fake.kt", newText).apply {
                when (danglingFileResolutionMode) {
                    KaDanglingFileResolutionMode.PREFER_SELF -> {}
                    KaDanglingFileResolutionMode.IGNORE_SELF -> {
                        originalFile = mainFile
                    }
                }
            }
        }

        doLazyResolveTest(fileToTest, testServices, OutputRenderingMode.ALL_FILES_FROM_ALL_MODULES) { resolutionFacade ->
            findFirDeclarationToResolve(
                ktFile = fileToTest,
                testServices = testServices,
                resolutionFacade = resolutionFacade,
                fileWithCaret = mainFile,
            )
        }
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            forTestsNotMatching("analysis/low-level-api-fir/testData/lazyResolve/noRuntime/*") {
                defaultDirectives {
                    +WITH_STDLIB
                }
            }

            forTestsMatching("analysis/low-level-api-fir/testData/lazyResolve/noRuntime/*") {
                defaultDirectives {
                    +NO_RUNTIME
                }
            }

            forTestsMatching("analysis/low-level-api-fir/testData/lazyResolve/withCallableMembers/*") {
                defaultDirectives {
                    LAZY_MODE.with(LazyResolveMode.WithCallableMembers)
                }
            }

            forTestsMatching("analysis/low-level-api-fir/testData/lazyResolve/danglingFile/ignoreSelf/*") {
                defaultDirectives {
                    Directives.DANGLING_FILE_RESOLUTION_MODE.with(KaDanglingFileResolutionMode.IGNORE_SELF)
                }
            }

            forTestsMatching("analysis/low-level-api-fir/testData/lazyResolve/danglingFile/preferSelf/*") {
                defaultDirectives {
                    Directives.DANGLING_FILE_RESOLUTION_MODE.with(KaDanglingFileResolutionMode.PREFER_SELF)
                }
            }
        }
    }
}

abstract class AbstractFirSourceLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractFirOutOfContentRootLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractFirOutOfContentRootWithDependenciesLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootWithDependenciesTestConfigurator
}

abstract class AbstractFirScriptLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractFirCustomScriptDefinitionLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirCustomScriptDefinitionTestConfigurator(
        analyseInDependentSession = false,
    )
}
