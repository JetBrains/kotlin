/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.psiUtil.siblings

internal object EditCommaSeparatedListHelper {
    public fun <TItem: JetElement> addItem(list: JetElement, allItems: List<TItem>, item: TItem): TItem {
        return addItemBefore(list, allItems, item, null)
    }

    public fun <TItem: JetElement> addItemAfter(list: JetElement, allItems: List<TItem>, item: TItem, anchor: TItem?): TItem {
        assert(anchor == null || anchor.getParent() == list)
        if (allItems.isEmpty()) {
            if (list.getFirstChild().getNode().getElementType() == JetTokens.LPAR) {
                return list.addAfter(item, list.getFirstChild()) as TItem
            }
            else {
                return list.add(item) as TItem
            }
        }
        else {
            var comma = JetPsiFactory(list).createComma()
            if (anchor != null) {
                comma = list.addAfter(comma, anchor)
                return list.addAfter(item, comma) as TItem
            }
            else {
                comma = list.addBefore(comma, allItems.first())
                return list.addBefore(item, comma) as TItem
            }
        }
    }

    public fun <TItem: JetElement> addItemBefore(list: JetElement, allItems: List<TItem>, item: TItem, anchor: TItem?): TItem {
        val anchorAfter: TItem?
        if (allItems.isEmpty()) {
            assert(anchor == null)
            anchorAfter = null
        }
        else {
            if (anchor != null) {
                val index = allItems.indexOf(anchor)
                assert(index >= 0)
                anchorAfter = if (index > 0) allItems.get(index - 1) else null
            }
            else {
                anchorAfter = allItems.get(allItems.size() - 1)
            }
        }
        return addItemAfter(list, allItems, item, anchorAfter)
    }

    public fun <TItem: JetElement> removeItem(item: TItem) {
        var comma = item.siblings(withItself = false).firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
        if (comma?.getNode()?.getElementType() != JetTokens.COMMA) {
            comma = item.siblings(forward = false, withItself = false).firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
            if (comma?.getNode()?.getElementType() != JetTokens.COMMA) {
                comma = null
            }
        }

        item.delete()
        comma?.delete()
    }
}
