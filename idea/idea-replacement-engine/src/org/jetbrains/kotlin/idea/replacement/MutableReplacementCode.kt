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

package org.jetbrains.kotlin.idea.replacement

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

internal class MutableReplacementCode(
        var mainExpression: KtExpression?,
        val statementsBefore: MutableList<KtExpression>,
        val fqNamesToImport: MutableCollection<FqName>
) {
    private val _postInsertionActions = HashMap<KtExpression, SmartList<(KtExpression) -> KtExpression>>()

    val postInsertionActions: Map<KtExpression, List<(KtExpression) -> KtExpression>>
        get() = _postInsertionActions

    fun <TStatement : KtExpression> addPostInsertionAction(statementBefore: TStatement, action: (TStatement) -> TStatement) {
        assert(statementBefore in statementsBefore)
        @Suppress("UNCHECKED_CAST")
        _postInsertionActions.getOrPut(statementBefore) { SmartList() }.add(action as (KtExpression) -> KtExpression)
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
        get() = statementsBefore + mainExpression.singletonOrEmptyList()

    operator fun contains(element: PsiElement): Boolean {
        return expressions.any { it.isAncestor(element) }
    }

    fun containsStrictlyInside(element: PsiElement): Boolean {
        return expressions.any { it.isAncestor(element, strict = true) }
    }
}

internal fun ReplacementCode.toMutable(): MutableReplacementCode {
    return MutableReplacementCode(
            mainExpression?.copied(),
            statementsBefore.map { it.copied() }.toMutableList(),
            fqNamesToImport.toMutableSet())
}

internal fun MutableReplacementCode.toNonMutable(): ReplacementCode {
    return ReplacementCode(mainExpression, statementsBefore, fqNamesToImport)
}

internal inline fun <reified T : PsiElement> MutableReplacementCode.collectDescendantsOfType(noinline predicate: (T) -> Boolean = { true }): List<T> {
    return expressions.flatMap { it.collectDescendantsOfType<T>({ true }, predicate) }
}

internal inline fun <reified T : PsiElement> MutableReplacementCode.forEachDescendantOfType(noinline action: (T) -> Unit) {
    expressions.forEach { it.forEachDescendantOfType<T>(action) }
}

