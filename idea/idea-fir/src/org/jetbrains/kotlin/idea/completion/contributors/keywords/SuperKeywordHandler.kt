/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors.keywords

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.completion.KeywordLookupObject
import org.jetbrains.kotlin.idea.completion.contributors.helpers.FirSuperEntriesProvider.getSuperClassesAvailableForSuperCall
import org.jetbrains.kotlin.idea.completion.contributors.helpers.SuperCallLookupObject
import org.jetbrains.kotlin.idea.completion.contributors.helpers.SuperCallInsertionHandler
import org.jetbrains.kotlin.idea.completion.createKeywordElement
import org.jetbrains.kotlin.idea.completion.keywords.CompletionKeywordHandler
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression

internal object SuperKeywordHandler : CompletionKeywordHandler<KtAnalysisSession>(KtTokens.SUPER_KEYWORD) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun KtAnalysisSession.createLookups(
        parameters: CompletionParameters,
        expression: KtExpression?,
        lookup: LookupElement,
        project: Project
    ): Collection<LookupElement> {
        val superClasses = getSuperClassesAvailableForSuperCall(parameters.position)

        if (superClasses.isEmpty()) {
            return emptyList()
        }

        if (expression == null) {
            // for completion in secondary constructor delegation call
            return listOf(lookup)
        }

        return when {
            superClasses.size <= 1 -> listOf(lookup)
            else -> buildList {
                add(lookup)
                superClasses.mapTo(this) { symbol ->
                    createKeywordElement("super", "<${symbol.name}>", SuperKeywordLookupObject(symbol.name, symbol.classIdIfNonLocal))
                        .withInsertHandler(SuperCallInsertionHandler)
                }
            }
        }
    }
}

private class SuperKeywordLookupObject(val className: Name, val classId: ClassId?) : KeywordLookupObject(), SuperCallLookupObject {
    override val replaceTo: String?
        get() = classId?.let { "super<${it.asSingleFqName().asString()}>" }

    override val shortenReferencesInReplaced: Boolean
        get() = classId != null
}

