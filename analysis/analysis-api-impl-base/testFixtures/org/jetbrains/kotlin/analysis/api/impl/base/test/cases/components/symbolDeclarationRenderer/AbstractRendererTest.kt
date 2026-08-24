/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.render
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.analysis.test.data.manager.withAdditionalVariant
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractRendererTest : AbstractAnalysisApiBasedTest() {
    override val variantChain: TestVariantChain
        get() = super.variantChain.withAdditionalVariant("new")

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + RendererDirectives

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val directives = testServices.moduleStructure.allDirectives

        val actual = executeOnPooledThreadInReadAction {
            buildString {
                // Since we want to render the whole file, we shouldn't use `IGNORE_SELF` as it's only designed for local use cases.
                copyAwareAnalyzeForTest(
                    mainFile,
                    danglingFileResolutionMode = KaDanglingFileResolutionMode.PREFER_SELF,
                ) { contextFile ->
                    val renderer = KaRenderer.default.copy {
                        applyDirectives(directives)

                        // Reproduce the legacy member ordering (see `AbstractLegacyRenderingTest`).
                        set(KaRenderingOption.ClassMemberOrdering) { first, second ->
                            fun renderToString(symbol: KaSymbol): String =
                                KaRenderingOutput.plainString().also { KaRenderer.default.render(symbol, it) }.toString()

                            renderToString(first).compareTo(renderToString(second))
                        }
                    }

                    contextFile.declarations.forEach { declaration ->
                        val output = KaRenderingOutput.plainString(indentationUnit = "  ")
                        renderer.render(declaration.symbol, output)
                        append(output.toString())
                        appendLine()
                        appendLine()
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = ".rendered")
    }

    private fun KaRendererBuilder.applyDirectives(directives: RegisteredDirectives) {
        set(KaRenderingOption.FlexibleTypeShrinking, RendererDirectives.NO_FLEXIBLE_TYPE_SHRINKING !in directives)

        if (RendererDirectives.NO_PRIMARY_CONSTRUCTOR_IN_CLASS_HEADER in directives) {
            set(KaRenderingOption.PrimaryConstructorInClassHeader, false)
        }

        if (RendererDirectives.NO_EXTRA_LINE_BETWEEN_MEMBERS in directives) {
            set(KaRenderingOption.ExtraLineBetweenMembers, false)
        }

        directives.singleOrZeroValue(RendererDirectives.CLASS_TYPE_QUALIFICATION)?.let {
            set(KaRenderingOption.ClassTypeQualification, it)
        }

        directives.singleOrZeroValue(RendererDirectives.CLASS_TYPE_RENDERING_MODE)?.let {
            set(KaRenderingOption.ClassTypeRenderingMode, it)
        }

        directives.singleOrZeroValue(RendererDirectives.TYPE_APPROXIMATION)?.let {
            set(KaRenderingOption.TypeApproximation, it)
        }
    }
}
