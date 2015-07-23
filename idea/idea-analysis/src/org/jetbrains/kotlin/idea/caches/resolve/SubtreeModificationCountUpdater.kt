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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

public class SubtreeModificationCountUpdater : PsiTreeChangePreprocessor {
    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        if (event.file !is JetFile) return

        when (event.code!!) {
            PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILDREN_CHANGE,
            PsiTreeChangeEventImpl.PsiEventType.BEFORE_PROPERTY_CHANGE,
            PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_MOVEMENT,
            PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_REPLACEMENT,
            PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_ADDITION,
            PsiTreeChangeEventImpl.PsiEventType.BEFORE_CHILD_REMOVAL,
            PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED -> {
                // skip
            }

            PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED,
            PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED -> {
                incCounters(event.parent)
            }

            PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED -> {
                if (!event.isGenericChange) {
                    incCounters(event.parent)
                }
            }

            PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED -> {
                incCounters(event.oldParent)
                incCounters(event.newParent)
            }

            else -> error("Unknown code:${event.code}")
        }
    }

    private fun incCounters(element: PsiElement?) {
        element?.parentsWithSelf?.forEach {
            val count = it.getUserData(MODIFICATION_COUNT_KEY)
            if (count != null) {
                it.putUserData(MODIFICATION_COUNT_KEY, count + 1)
            }
        }
    }

    companion object {
        private val MODIFICATION_COUNT_KEY = Key<Int>("SubtreeModificationCountUpdater.MODIFICATION_COUNT_KEY")

        public fun getModificationCount(element: PsiElement): Int {
            element.getUserData(MODIFICATION_COUNT_KEY)?.let { return it }

            element.putUserData(MODIFICATION_COUNT_KEY, 0)
            return 0
        }
    }
}