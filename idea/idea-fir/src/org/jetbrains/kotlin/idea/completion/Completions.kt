/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import org.jetbrains.kotlin.idea.completion.context.*
import org.jetbrains.kotlin.idea.completion.contributors.FirCompletionContributorFactory
import org.jetbrains.kotlin.idea.completion.contributors.complete
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession

internal object Completions {
    fun KtAnalysisSession.complete(
        factory: FirCompletionContributorFactory,
        positionContext: FirRawPositionCompletionContext,
    ) {
        when (positionContext) {
            is FirExpressionNameReferencePositionContext -> {
                complete(factory.keywordContributor(0), positionContext)
                complete(factory.callableContributor(0), positionContext)
                complete(factory.classifierContributor(0), positionContext)
                complete(factory.packageCompletionContributor(1), positionContext)
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
}