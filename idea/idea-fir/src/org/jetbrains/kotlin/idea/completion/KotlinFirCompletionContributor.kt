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
import org.jetbrains.kotlin.idea.completion.contributors.complete
import org.jetbrains.kotlin.idea.completion.weighers.Weighers
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalKtFile
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile

class KotlinFirCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), KotlinFirCompletionProvider)
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val psiFile = context.file
        if (psiFile !is KtFile) return

        val identifierProviderService = service<CompletionDummyIdentifierProviderService>()

        correctPositionAndDummyIdentifier(identifierProviderService, context)
    }

    private fun correctPositionAndDummyIdentifier(
        identifierProviderService: CompletionDummyIdentifierProviderService,
        context: CompletionInitializationContext
    ) {
        val dummyIdentifierCorrected = identifierProviderService.correctPositionForStringTemplateEntry(context)
        if (dummyIdentifierCorrected) {
            return
        }

        context.dummyIdentifier = identifierProviderService.provideDummyIdentifier(context)
    }
}

private object KotlinFirCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        @Suppress("NAME_SHADOWING") val parameters = KotlinFirCompletionParametersProvider.provide(parameters)

        if (shouldSuppressCompletion(parameters.ijParameters, result.prefixMatcher)) return
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
        val factory = FirCompletionContributorFactory(basicContext)

        when (positionContext) {
            is FirExpressionNameReferencePositionContext -> {
                complete(factory.keywordContributor(1), positionContext)
                complete(factory.callableContributor(1), positionContext)
                complete(factory.classifierContributor(1), positionContext)
                complete(factory.packageCompletionContributor(2), positionContext)
            }

            is FirTypeNameReferencePositionContext -> {
                complete(factory.classifierContributor(0), positionContext)
                complete(factory.keywordContributor(1), positionContext)
                complete(factory.packageCompletionContributor(2), positionContext)
            }

            is FirAnnotationTypeNameReferencePositionContext -> {
                complete(factory.annotationsContributor(0), positionContext)
                complete(factory.keywordContributor(1), positionContext)
                complete(factory.packageCompletionContributor(2), positionContext)
            }

            is FirSuperTypeCallNameReferencePositionContext -> {
                complete(factory.superEntryContributor(0), positionContext)
            }

            is FirImportDirectivePositionContext -> {
                complete(factory.packageCompletionContributor(0), positionContext)
                complete(factory.importDirectivePackageMembersContributor(0), positionContext)
            }

            is FirPackageDirectivePositionContext -> {
                complete(factory.packageCompletionContributor(0), positionContext)
            }

            is FirTypeConstraintNameInWhereClausePositionContext -> {
                complete(factory.typeParameterConstraintNameInWhereClauseContributor(0), positionContext)
            }

            is FirUnknownPositionContext -> {
                complete(factory.keywordContributor(0), positionContext)
            }

            is FirClassifierNamePositionContext -> {
                complete(factory.classifierNameContributor(0), positionContext)
            }

            is FirWithSubjectEntryPositionContext -> {
                complete(factory.whenWithSubjectConditionContributor(0), positionContext)
            }

            is FirCallableReferencePositionContext -> {
                complete(factory.classReferenceContributor(0), positionContext)
                complete(factory.callableReferenceContributor(1), positionContext)
                complete(factory.classifierReferenceContributor(1), positionContext)
            }

            is FirInfixCallPositionContext -> {
                complete(factory.keywordContributor(0), positionContext)
                complete(factory.infixCallableContributor(0), positionContext)
            }

            is FirIncorrectPositionContext -> {
                // do nothing, completion is not supposed to be called here
            }
        }
    }


    private fun recordOriginalFile(basicCompletionContext: FirBasicCompletionContext) {
        val originalFile = basicCompletionContext.originalKtFile
        val fakeFile = basicCompletionContext.fakeKtFile
        fakeFile.originalKtFile = originalFile
    }

    private fun createResultSet(parameters: KotlinFirCompletionParameters, result: CompletionResultSet): CompletionResultSet {
        @Suppress("NAME_SHADOWING") var result = result.withRelevanceSorter(createSorter(parameters.ijParameters, result))
        if (parameters is KotlinFirCompletionParameters.Corrected) {
            val replaced = parameters.ijParameters

            @Suppress("UnstableApiUsage", "DEPRECATION")
            val originalPrefix = CompletionData.findPrefixStatic(replaced.position, replaced.offset)
            result = result.withPrefixMatcher(originalPrefix)
        }
        return result
    }

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
