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

package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.siblings

object EditCommaSeparatedListHelper {
    @JvmOverloads
    fun <TItem: KtElement> addItem(list: KtElement, allItems: List<TItem>, item: TItem, prefix: KtToken = KtTokens.LPAR): TItem {
        return addItemBefore(list, allItems, item, null, prefix)
    }

    @JvmOverloads
    fun <TItem: KtElement> addItemAfter(list: KtElement, allItems: List<TItem>, item: TItem, anchor: TItem?, prefix: KtToken = KtTokens.LPAR): TItem {
        assert(anchor == null || anchor.parent == list)
        if (allItems.isEmpty()) {
            return if (list.firstChild?.node?.elementType == prefix) {
                list.addAfter(item, list.firstChild) as TItem
            }
            else {
                list.add(item) as TItem
            }
        }
        else {
            var comma = KtPsiFactory(list).createComma()
            return if (anchor != null) {
                comma = list.addAfter(comma, anchor)
                list.addAfter(item, comma) as TItem
            }
            else {
                comma = list.addBefore(comma, allItems.first())
                list.addBefore(item, comma) as TItem
            }
        }
    }

    @JvmOverloads
    fun <TItem: KtElement> addItemBefore(list: KtElement, allItems: List<TItem>, item: TItem, anchor: TItem?, prefix: KtToken = KtTokens.LPAR): TItem {
        val anchorAfter: TItem?
        anchorAfter = if (allItems.isEmpty()) {
            assert(anchor == null)
            null
        }
        else {
            if (anchor != null) {
                val index = allItems.indexOf(anchor)
                assert(index >= 0)
                if (index > 0) allItems[index - 1] else null
            }
            else {
                allItems[allItems.size - 1]
            }
        }
        return addItemAfter(list, allItems, item, anchorAfter, prefix)
    }

    fun <TItem: KtElement> removeItem(item: TItem) {
        var comma = item.siblings(withItself = false).firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
        if (comma?.node?.elementType != KtTokens.COMMA) {
            comma = item.siblings(forward = false, withItself = false).firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
            if (comma?.node?.elementType != KtTokens.COMMA) {
                comma = null
            }
        }

        item.delete()
        comma?.delete()
    }
}
