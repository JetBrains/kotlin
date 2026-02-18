/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.references.utils.KotlinKDocResolutionStrategyProviderService

@SubclassOptInRequired(KtImplementationDetail::class)
abstract class KDocReference(element: KDocName) : KtMultiReference<KDocName>(element) {
    override fun getRangeInElement(): TextRange = element.getNameTextRange()

    override fun canRename(): Boolean = true

    override fun resolve(): PsiElement? = multiResolve(incompleteCode = false).let { resolvedResults ->
        if (KotlinKDocResolutionStrategyProviderService.getService(element.project)?.shouldUseExperimentalStrategy() == true) {
            /**
             * It's important to use [singleOrNull] instead of [firstOrNull] here
             * to get a drop-down menu in the IDE for KDoc references with multiple resolved results.
             * In cases when [resolve] returns `null`,
             * IDE will use [multiResolve] instead and show all the found results.
             * Otherwise, when some element is returned from [resolve],
             * the IDE considers it to be the primary result and just shows it as-is.
             *
             * This logic should only be used if both the K2 mode and the experimental resolution are enabled.
             * See KT-76607.
             */
            resolvedResults.singleOrNull()
        } else {
            resolvedResults.firstOrNull()
        }
    }?.element

    override fun getCanonicalText(): String = element.getNameText()

    override val resolvesByNames: Collection<Name>
        get() {
            val element = element
            val name = element.getNameText()

            // Text check is required to distinguish between '`this`'/'`super`' and 'this'/'super' cases
            if (name in FORBIDDEN_NAMES && element.textMatches(name)) {
                // According to the KDoc, `this`/`super` cannot be properly expressed in terms of this API
                return emptyList()
            }

            return listOfNotNull(
                Name.identifier(name),
                // A property might resolve into a getter function
                JvmAbi.getterName(name).takeIf { it != name }?.let(Name::identifier),
            )
        }
}

private val FORBIDDEN_NAMES = listOf(KtTokens.THIS_KEYWORD.value, KtTokens.SUPER_KEYWORD.value)
