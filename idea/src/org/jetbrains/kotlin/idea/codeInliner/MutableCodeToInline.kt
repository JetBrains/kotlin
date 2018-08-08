/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

private val POST_INSERTION_ACTION: Key<(KtElement) -> Unit> = Key("POST_INSERTION_ACTION")

internal class MutableCodeToInline(
    var mainExpression: KtExpression?,
    val statementsBefore: MutableList<KtExpression>,
    val fqNamesToImport: MutableCollection<FqName>,
    val alwaysKeepMainExpression: Boolean
) {
    fun <TElement : KtElement> addPostInsertionAction(element: TElement, action: (TElement) -> Unit) {
        assert(element in this)
        @Suppress("UNCHECKED_CAST")
        element.putCopyableUserData(POST_INSERTION_ACTION, action as (KtElement) -> Unit)
    }

    fun performPostInsertionActions(elements: Collection<PsiElement>) {
        for (element in elements) {
            element.forEachDescendantOfType<KtElement> {
                val action = it.getCopyableUserData(POST_INSERTION_ACTION)
                if (action != null) {
                    it.putCopyableUserData(POST_INSERTION_ACTION, null)
                    action.invoke(it)
                }
            }
        }
    }

    fun replaceExpression(oldExpression: KtExpression, newExpression: KtExpression): KtExpression {
        assert(oldExpression in this)

        if (oldExpression == mainExpression) {
            mainExpression = newExpression
            return newExpression
        }

        val index = statementsBefore.indexOf(oldExpression)
        if (index >= 0) {
            statementsBefore[index] = newExpression
            return newExpression
        }

        return oldExpression.replace(newExpression) as KtExpression
    }

    val expressions: Collection<KtExpression>
        get() = statementsBefore + listOfNotNull(mainExpression)

    operator fun contains(element: PsiElement): Boolean {
        return expressions.any { it.isAncestor(element) }
    }

    fun containsStrictlyInside(element: PsiElement): Boolean {
        return expressions.any { it.isAncestor(element, strict = true) }
    }
}

internal fun CodeToInline.toMutable(): MutableCodeToInline {
    return MutableCodeToInline(
        mainExpression?.copied(),
        statementsBefore.asSequence().map { it.copied() }.toMutableList(),
        fqNamesToImport.toMutableSet(),
        alwaysKeepMainExpression
    )
}

internal fun MutableCodeToInline.toNonMutable(): CodeToInline {
    return CodeToInline(mainExpression, statementsBefore, fqNamesToImport, alwaysKeepMainExpression)
}

internal inline fun <reified T : PsiElement> MutableCodeToInline.collectDescendantsOfType(noinline predicate: (T) -> Boolean = { true }): List<T> {
    return expressions.flatMap { it.collectDescendantsOfType<T>({ true }, predicate) }
}

internal inline fun <reified T : PsiElement> MutableCodeToInline.forEachDescendantOfType(noinline action: (T) -> Unit) {
    expressions.forEach { it.forEachDescendantOfType<T>(action) }
}

