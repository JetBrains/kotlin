/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfTypeSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.utils.ignoreExceptionIfIgnoreDirectivePresent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class AbstractClassIdTest : AbstractAnalysisApiBasedTest() {
    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_CONSISTENCY_CHECK by stringDirective("Temporary disable test until the issue is fixed")
    }

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        var hasInconsistencies = false
        val text = buildString {
            withResolutionFacade(mainFile) { resolutionFacade ->
                mainFile.accept(object : PsiElementVisitor() {
                    override fun visitElement(element: PsiElement) {
                        if (element is KtClassLikeDeclaration) {
                            val psiClassId = element.getClassId()
                            val error = validateClassId(element, resolutionFacade, psiClassId)
                            val suffix = if (error != null) {
                                hasInconsistencies = true
                                "$error "
                            } else {
                                ""
                            }

                            append("/* ClassId: $psiClassId $suffix*/")
                        }

                        if (element is LeafPsiElement) {
                            append(element.text)
                        }

                        element.acceptChildren(this)
                    }

                    override fun visitComment(comment: PsiComment) {
                        if (comment.tokenType == KtTokens.BLOCK_COMMENT) {
                            return
                        }

                        super.visitComment(comment)
                    }
                })
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(text, mainFile.name.substringAfterLast('.'))
        testServices.moduleStructure.allDirectives.ignoreExceptionIfIgnoreDirectivePresent(Directives.IGNORE_CONSISTENCY_CHECK) {
            if (hasInconsistencies) {
                error("The test has inconsistencies. Please fix them or add '// ${Directives.IGNORE_CONSISTENCY_CHECK}' directive with the corresponding YT issue number to mute the problem.")
            }
        }
    }

    private fun validateClassId(
        element: KtClassLikeDeclaration,
        resolutionFacade: LLResolutionFacade,
        psiClassId: ClassId?,
    ): String? = listOfNotNull(
        comparePsiClassIdWithFirClassId(element, resolutionFacade, psiClassId),
        comparePsiClassIdWithPsiFqName(element, psiClassId),
    ).ifNotEmpty { joinToString(prefix = "[", postfix = "]") }

    private fun comparePsiClassIdWithFirClassId(
        element: KtClassLikeDeclaration,
        resolutionFacade: LLResolutionFacade,
        psiClassId: ClassId?,
    ): String? {
        val firSymbolResult = runCatching {
            element.resolveToFirSymbolOfTypeSafe<FirClassLikeSymbol<*>>(resolutionFacade)
        }

        return if (firSymbolResult.isFailure) {
            val exception = firSymbolResult.exceptionOrNull()!!
            "${exception::class.simpleName}: ${exception.message}"
        } else {
            val firClassId = firSymbolResult.getOrNull()?.classId?.takeUnless {
                @OptIn(ClassIdBasedLocality::class)
                it.isLocal
            }

            if (psiClassId == firClassId) null else "FirClassId: $firClassId"
        }
    }

    private fun comparePsiClassIdWithPsiFqName(declaration: KtClassLikeDeclaration, psiClassId: ClassId?): String? {
        val fqName = declaration.safeFqNameForLazyResolve()
        return if (psiClassId?.asSingleFqName() != fqName) {
            "PsiFqName: $fqName"
        } else {
            null
        }
    }
}

abstract class AbstractSourceLikeClassIdTest : AbstractClassIdTest() {
    override val configurator: AnalysisApiTestConfigurator = LLSourceLikeTestConfigurator()
}
