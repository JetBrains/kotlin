/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.findUsages

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usages.Usage
import com.intellij.usages.rules.ImportFilteringRule
import com.intellij.usages.rules.PsiElementUsage
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

public class JetImportFilteringRule : ImportFilteringRule() {
    public override fun isVisible(usage: Usage): Boolean {
        if (usage is PsiElementUsage) {
            return usage.getElement()?.getNonStrictParentOfType<JetImportDirective>() == null
        }

        return true
    }
}
