/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.components.service
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.completion.context.*
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirExpressionNameReferencePositionContext
import org.jetbrains.kotlin.idea.completion.context.FirPositionCompletionContextDetector
import org.jetbrains.kotlin.idea.completion.context.FirTypeNameReferencePositionContext
import org.jetbrains.kotlin.idea.completion.context.FirUnknownPositionContext
import org.jetbrains.kotlin.idea.completion.contributors.*
import org.jetbrains.kotlin.idea.completion.contributors.FirAnnotationCompletionContributor
import org.jetbrains.kotlin.idea.completion.contributors.FirCallableCompletionContributor
import org.jetbrains.kotlin.idea.completion.contributors.FirClassifierCompletionContributor
import org.jetbrains.kotlin.idea.completion.contributors.FirKeywordCompletionContributor
import org.jetbrains.kotlin.idea.completion.contributors.complete
import org.jetbrains.kotlin.idea.completion.weighers.Weighers
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalKtFile
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class KotlinFirCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), KotlinFirCompletionProvider)
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val psiFile = context.file
        if (psiFile !is KtFile) return

        context.dummyIdentifier = service<CompletionDummyIdentifierProviderService>().provideDummyIdentifier(context)
    }
}

private object KotlinFirCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        if (shouldSuppressCompletion(parameters, result.prefixMatcher)) return

        val resultSet = createResultSet(parameters, result)

        val basicContext = FirBasicCompletionContext.createFromParameters(parameters, resultSet) ?: return
        recordOriginalFile(basicContext)

        val positionContext = FirPositionCompletionContextDetector.detect(basicContext)

        FirPositionCompletionContextDetector.analyseInContext(basicContext, positionContext) {
            complete(basicContext, positionContext)
        }
    }


    private fun KtAnalysisSession.complete(
        basicContext: FirBasicCompletionContext,
        positionContext: FirRawPositionCompletionContext,
    ) {
        val keywordContributor = FirKeywordCompletionContributor(basicContext)
        val callableContributor = FirCallableCompletionContributor(basicContext)
        val classifierContributor = FirClassifierCompletionContributor(basicContext)
        val annotationsContributor = FirAnnotationCompletionContributor(basicContext)
        val packageCompletionContributor = FirPackageCompletionContributor(basicContext)
        val importDirectivePackageMembersCompletionContributor = FirImportDirectivePackageMembersCompletionContributor(basicContext)
        val typeParameterConstraintNameInWhereClauseContributor = FirTypeParameterConstraintNameInWhereClauseCompletionContributor(basicContext)
        val classifierNameContributor = FirSameAsFileClassifierNameCompletionContributor(basicContext)

        when (positionContext) {
            is FirExpressionNameReferencePositionContext -> {
                complete(keywordContributor, positionContext)
                complete(packageCompletionContributor, positionContext)
                complete(callableContributor, positionContext)
                complete(classifierContributor, positionContext)
            }

            is FirTypeNameReferencePositionContext -> {
                complete(keywordContributor, positionContext)
                complete(packageCompletionContributor, positionContext)
                complete(classifierContributor, positionContext)
            }

            is FirAnnotationTypeNameReferencePositionContext -> {
                complete(keywordContributor, positionContext)
                complete(packageCompletionContributor, positionContext)
                complete(annotationsContributor, positionContext)
            }

            is FirSuperTypeCallNameReferencePositionContext -> {
                complete(FirSuperEntryContributor(basicContext), positionContext)
            }

            is FirImportDirectivePositionContext -> {
                complete(packageCompletionContributor, positionContext)
                complete(importDirectivePackageMembersCompletionContributor, positionContext)
            }

            is FirPackageDirectivePositionContext -> {
                complete(packageCompletionContributor, positionContext)
            }

            is FirTypeConstraintNameInWhereClausePositionContext -> {
                complete(typeParameterConstraintNameInWhereClauseContributor, positionContext)
            }

            is FirUnknownPositionContext -> {
                complete(keywordContributor, positionContext)
            }

            is FirClassifierNamePositionContext -> {
                complete(classifierNameContributor, positionContext)
            }

            is FirIncorrectPositionContext -> {
                // do nothing, completion is not suposed to be called here
            }
        }
    }


    private fun recordOriginalFile(basicCompletionContext: FirBasicCompletionContext) {
        val originalFile = basicCompletionContext.originalKtFile
        val fakeFile = basicCompletionContext.fakeKtFile
        fakeFile.originalKtFile = originalFile
    }

    private fun createResultSet(parameters: CompletionParameters, result: CompletionResultSet): CompletionResultSet =
        result.withRelevanceSorter(createSorter(parameters, result))

    private fun createSorter(parameters: CompletionParameters, result: CompletionResultSet): CompletionSorter =
        CompletionSorter.defaultSorter(parameters, result.prefixMatcher)
            .let(Weighers::addWeighersToCompletionSorter)

    private val AFTER_NUMBER_LITERAL = PsiJavaPatterns.psiElement().afterLeafSkipping(
        PsiJavaPatterns.psiElement().withText(""),
        PsiJavaPatterns.psiElement().withElementType(PsiJavaPatterns.elementType().oneOf(KtTokens.FLOAT_LITERAL, KtTokens.INTEGER_LITERAL))
    )
    private val AFTER_INTEGER_LITERAL_AND_DOT = PsiJavaPatterns.psiElement().afterLeafSkipping(
        PsiJavaPatterns.psiElement().withText("."),
        PsiJavaPatterns.psiElement().withElementType(PsiJavaPatterns.elementType().oneOf(KtTokens.INTEGER_LITERAL))
    )

    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.position
        val invocationCount = parameters.invocationCount

        // no completion inside number literals
        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // no completion auto-popup after integer and dot
        if (invocationCount == 0 && prefixMatcher.prefix.isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        return false
    }
}
