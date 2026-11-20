/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.stubs.StubElement
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

/**
 * Checks class IDs of class-like declarations against their FIR symbols and fully qualified names.
 *
 * The class IDs are computed twice: on top of the AST, and then on top of the stubs of the same file, which is how the IDE mostly sees
 * declarations. Stub-based results that differ from the AST-based ones are reported as inconsistencies.
 */
abstract class AbstractClassIdTest : AbstractAnalysisApiBasedTest() {
    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_CONSISTENCY_CHECK by stringDirective("Temporary disable test until the issue is fixed")

        val IGNORE_TREE_ACCESS by stringDirective(
            "Disables the stub-based part of the test, which loads the AST. The YT issue number has to be provided"
        )
    }

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        // The file is created from the text, so it has no stubs yet.
        // Declarations are resolved in pre-order, as the resolution order affects the symbol provider caches.
        val astBasedClassIds = withResolutionFacade(mainFile) { resolutionFacade ->
            buildMap {
                mainFile.accept(object : PsiRecursiveElementVisitor() {
                    override fun visitElement(element: PsiElement) {
                        if (element is KtClassLikeDeclaration) {
                            put(element.textRange.startOffset, computeClassId(element, resolutionFacade, isStubBased = false))
                        }

                        super.visitElement(element)
                    }
                })
            }
        }

        var stubBasedClassIds: Map<Int, ClassIdResult>? = null
        testServices.moduleStructure.allDirectives.ignoreExceptionIfIgnoreDirectivePresent(Directives.IGNORE_TREE_ACCESS) {
            stubBasedClassIds = computeStubBasedClassIds(mainFile, testServices)
        }

        var hasInconsistencies = false
        val text = buildString {
            mainFile.accept(object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is KtClassLikeDeclaration) {
                        val offset = element.textRange.startOffset
                        val astBasedClassId = astBasedClassIds.getValue(offset)
                        val error = validateClassId(element, astBasedClassId, stubBasedClassIds?.get(offset))
                        val suffix = if (error != null) {
                            hasInconsistencies = true
                            "$error "
                        } else {
                            ""
                        }

                        append("/* ClassId: ${astBasedClassId.psiClassId} $suffix*/")
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

        testServices.assertions.assertEqualsToTestOutputFile(text, mainFile.name.substringAfterLast('.'))
        testServices.moduleStructure.allDirectives.ignoreExceptionIfIgnoreDirectivePresent(Directives.IGNORE_CONSISTENCY_CHECK) {
            if (hasInconsistencies) {
                error("The test has inconsistencies. Please fix them or add '// ${Directives.IGNORE_CONSISTENCY_CHECK}' directive with the corresponding YT issue number to mute the problem.")
            }
        }
    }

    /**
     * Puts [file] into a stub-based state and computes class IDs of its stub-based declarations without loading the AST.
     * The AST is loaded afterward, so the result is keyed by the start offsets of the declarations.
     */
    private fun computeStubBasedClassIds(file: KtFile, testServices: TestServices): Map<Int, ClassIdResult> {
        forceStubTree(file, testServices)

        // FIR of the AST-based file must not be reused
        clearCaches(file.project)

        val stubBasedClassIds = withAstLoadingAssertion(file) {
            withResolutionFacade(file) { resolutionFacade ->
                buildList {
                    fun visit(stub: StubElement<*>) {
                        (stub.psi as? KtClassLikeDeclaration)?.let { add(it to computeClassId(it, resolutionFacade, isStubBased = true)) }
                        stub.childrenStubs.forEach(::visit)
                    }

                    visit(file.stub ?: error("Stub should be present for unloaded file"))
                }
            }
        }

        file.calcTreeElement()
        return stubBasedClassIds.associate { [declaration, classId] -> declaration.textRange.startOffset to classId }
    }

    private fun computeClassId(
        declaration: KtClassLikeDeclaration,
        resolutionFacade: LLResolutionFacade,
        isStubBased: Boolean,
    ): ClassIdResult {
        val psiClassId = declaration.getClassId()

        // A declaration without a class ID is resolved via the body of its container, which requires the AST
        if (isStubBased && psiClassId == null) {
            return ClassIdResult(psiClassId, firInconsistency = null, isResolved = false)
        }

        return ClassIdResult(psiClassId, comparePsiClassIdWithFirClassId(declaration, resolutionFacade, psiClassId), isResolved = true)
    }

    private fun validateClassId(
        element: KtClassLikeDeclaration,
        astBasedClassId: ClassIdResult,
        stubBasedClassId: ClassIdResult?,
    ): String? = listOfNotNull(
        astBasedClassId.firInconsistency,
        comparePsiClassIdWithPsiFqName(element, astBasedClassId.psiClassId),
        stubBasedClassId?.takeIf { it.psiClassId != astBasedClassId.psiClassId }?.let { "StubClassId: ${it.psiClassId}" },
        stubBasedClassId?.takeIf { it.isResolved && it.firInconsistency != astBasedClassId.firInconsistency }?.let {
            "StubFirInconsistency: ${it.firInconsistency}"
        },
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

            // AST loading in the stub-based part is a failure of the whole part, not of a single declaration
            if (exception is AstLoadingAssertionError) {
                throw exception
            }

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

    /**
     * @param isResolved `false` if [firInconsistency] is not computed, as the declaration cannot be resolved on stubs.
     */
    private class ClassIdResult(val psiClassId: ClassId?, val firInconsistency: String?, val isResolved: Boolean)
}

abstract class AbstractSourceLikeClassIdTest : AbstractClassIdTest() {
    override val configurator: AnalysisApiTestConfigurator = LLSourceLikeTestConfigurator()
}
