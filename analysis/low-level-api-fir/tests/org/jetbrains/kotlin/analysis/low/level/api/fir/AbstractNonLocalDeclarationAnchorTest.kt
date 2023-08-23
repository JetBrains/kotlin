/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractNonLocalDeclarationAnchorTest : AbstractLowLevelApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val anchors = hashSetOf<KtDeclaration>()
        ktFile.forEachDescendantOfType<PsiElement> {
            it.getNonLocalContainingOrThisDeclaration()?.let(anchors::add)
        }

        val text = buildString {
            ktFile.accept(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val isAnchor = element in anchors
                    if (isAnchor) {
                        append("/* anchor --> */")
                    }

                    if (element is LeafPsiElement) {
                        append(element.text)
                    }

                    element.acceptChildren(this)
                    if (isAnchor) {
                        append("/* <-- */")
                    }
                }

                override fun visitComment(comment: PsiComment) {}
            })
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(text)
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            defaultDirectives {
                +ConfigurationDirectives.WITH_STDLIB
            }
        }
    }
}

abstract class AbstractSourceNonLocalDeclarationAnchorTest : AbstractNonLocalDeclarationAnchorTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractScriptNonLocalDeclarationAnchorTest : AbstractNonLocalDeclarationAnchorTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
