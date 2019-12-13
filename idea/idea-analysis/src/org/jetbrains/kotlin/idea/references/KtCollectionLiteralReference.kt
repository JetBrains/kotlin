/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CollectionLiteralResolver

class KtCollectionLiteralReference(expression: KtCollectionLiteralExpression) :
    KtSimpleReference<KtCollectionLiteralExpression>(expression), MultiRangeReference {
    companion object {
        private val COLLECTION_LITERAL_CALL_NAMES =
            CollectionLiteralResolver.PRIMITIVE_TYPE_TO_ARRAY.values + CollectionLiteralResolver.ARRAY_OF_FUNCTION
    }

    override fun getRangeInElement(): TextRange = element.normalizeRange()

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val resolvedCall = context[BindingContext.COLLECTION_LITERAL_CALL, element]
        return listOfNotNull(resolvedCall?.resultingDescriptor)
    }

    override fun getRanges(): List<TextRange> {
        return listOfNotNull(element.leftBracket?.normalizeRange(), element.rightBracket?.normalizeRange())
    }

    override val resolvesByNames: Collection<Name>
        get() = COLLECTION_LITERAL_CALL_NAMES

    private fun PsiElement.normalizeRange(): TextRange = this.textRange.shiftRight(-expression.textOffset)
}
