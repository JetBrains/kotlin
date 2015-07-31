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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import org.jetbrains.kotlin.idea.references.BuiltInsReferenceResolver
import org.jetbrains.kotlin.psi.JetElement

public class BuiltInsUseScopeEnlarger : UseScopeEnlarger() {
    override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
        if (element is JetElement && BuiltInsReferenceResolver.isFromBuiltIns(element)) {
            return ProjectScope.getAllScope(element.getProject())
        }
        return null
    }
}