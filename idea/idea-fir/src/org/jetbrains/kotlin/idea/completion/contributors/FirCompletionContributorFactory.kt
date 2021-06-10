/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext

internal class FirCompletionContributorFactory(private val basicContext: FirBasicCompletionContext) {
    fun keywordContributor(priority: Int = 0) =
        FirKeywordCompletionContributor(basicContext, priority)

    fun classReferenceContributor(priority: Int = 0) =
        FirClassReferenceCompletionContributor(basicContext, priority)

    fun callableContributor(priority: Int = 0) =
        FirCallableCompletionContributor(basicContext, priority)

    fun infixCallableContributor(priority: Int = 0) =
        FirInfixCallableCompletionContributor(basicContext, priority)

    fun callableReferenceContributor(priority: Int = 0) =
        FirCallableReferenceCompletionContributor(basicContext, priority)

    fun classifierContributor(priority: Int = 0) =
        FirClassifierCompletionContributor(basicContext, priority)

    fun classifierReferenceContributor(priority: Int = 0) =
        FirClassifierReferenceCompletionContributor(basicContext, priority)

    fun annotationsContributor(priority: Int = 0) =
        FirAnnotationCompletionContributor(basicContext, priority)

    fun packageCompletionContributor(priority: Int = 0) =
        FirPackageCompletionContributor(basicContext, priority)

    fun importDirectivePackageMembersContributor(priority: Int = 0) =
        FirImportDirectivePackageMembersCompletionContributor(basicContext, priority)

    fun typeParameterConstraintNameInWhereClauseContributor(priority: Int = 0) =
        FirTypeParameterConstraintNameInWhereClauseCompletionContributor(basicContext, priority)

    fun classifierNameContributor(priority: Int = 0) =
        FirSameAsFileClassifierNameCompletionContributor(basicContext, priority)

    fun whenWithSubjectConditionContributor(priority: Int = 0) =
        FirWhenWithSubjectConditionContributor(basicContext, priority)

    fun superEntryContributor(priority: Int) =
        FirSuperEntryContributor(basicContext, priority)
}