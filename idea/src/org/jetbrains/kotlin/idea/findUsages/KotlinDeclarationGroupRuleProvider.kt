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

import com.intellij.usages.rules.PsiElementUsage
import com.intellij.usages.UsageGroup
import com.intellij.usages.Usage
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usages.rules.UsageGroupingRule
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import com.intellij.usages.PsiNamedElementUsageGroupBase
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.usages.impl.FileStructureGroupRuleProvider
import com.intellij.openapi.project.Project

public class KotlinDeclarationGroupRuleProvider : FileStructureGroupRuleProvider {
    public class KotlinDeclarationGroupingRule : UsageGroupingRule {
        override fun groupUsage(usage: Usage): UsageGroup? {
            val element = (usage as? PsiElementUsage)?.getElement()
            if (element == null) return null

            val containingFile = element.getContainingFile()
            if (containingFile !is JetFile) return null

            return PsiTreeUtil.getTopmostParentOfType(element, javaClass<JetNamedDeclaration>())?.let { container ->
                PsiNamedElementUsageGroupBase(container)
            }
        }
    }

    override fun getUsageGroupingRule(project: Project): UsageGroupingRule = KotlinDeclarationGroupingRule()
}


