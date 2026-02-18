/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.resolve.CollectionNames
import org.jetbrains.kotlin.util.OperatorNameConventions

@OptIn(KtExperimentalApi::class)
@SubclassOptInRequired(KtImplementationDetail::class)
abstract class KtCollectionLiteralReference(
    expression: KtCollectionLiteralExpression,
) : KtSimpleReference<KtCollectionLiteralExpression>(expression), MultiRangeReference, KtResolvable {
    companion object {
        private val COLLECTION_LITERAL_CALL_NAMES: List<Name> = buildList {
            addAll(ArrayFqNames.ARRAY_CALL_NAMES)
            addAll(CollectionNames.Factories.NAMES)
            add(OperatorNameConventions.OF)
        }.sorted()
    }

    override fun getRangeInElement(): TextRange = element.normalizeRange()

    override fun getRanges(): List<TextRange> {
        return listOfNotNull(element.leftBracket?.normalizeRange(), element.rightBracket?.normalizeRange())
    }

    override val resolvesByNames: Collection<Name>
        get() = COLLECTION_LITERAL_CALL_NAMES

    private fun PsiElement.normalizeRange(): TextRange = this.textRange.shiftRight(-expression.textOffset)
}
