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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.ChangeLocalityDetector
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.trackers.KotlinCodeBlockModificationListenerCompat.Companion.getInsideCodeBlockModificationDirtyScope

class KotlinChangeLocalityDetector : ChangeLocalityDetector {
    override fun getChangeHighlightingDirtyScopeFor(element: PsiElement): PsiElement? {
        // in some cases it returns a bit wider scope for the element as it is not possible to track changes here
        // e.g.: delete a space in expression `foo( )` results to entire expression `foo()`
        return getInsideCodeBlockModificationDirtyScope(element)
    }
}