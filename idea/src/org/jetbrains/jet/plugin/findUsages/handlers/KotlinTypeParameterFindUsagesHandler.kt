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

package org.jetbrains.jet.plugin.findUsages.handlers

import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.plugin.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.jet.plugin.findUsages.dialogs.KotlinTypeParameterFindUsagesDialog
import org.jetbrains.jet.plugin.search.usagesSearch.DefaultSearchHelper
import org.jetbrains.jet.plugin.findUsages.toSearchTarget
import org.jetbrains.jet.plugin.search.usagesSearch.search
import org.jetbrains.jet.plugin.refactoring.runReadAction

public class KotlinTypeParameterFindUsagesHandler(
        element: JetNamedDeclaration,
        factory: KotlinFindUsagesHandlerFactory
) : KotlinFindUsagesHandler<JetNamedDeclaration>(element, factory) {
    public override fun getFindUsagesDialog(
            isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean
    ): AbstractFindUsagesDialog {
        return KotlinTypeParameterFindUsagesDialog<JetNamedDeclaration>(
                getElement(), getProject(), getFindUsagesOptions(), toShowInNewTab, mustOpenInNewTab, isSingleFile, this
        )
    }

    protected override fun searchReferences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        return runReadAction {
            val target = options.toSearchTarget(element as JetNamedDeclaration, true)
            val request = DefaultSearchHelper<JetNamedDeclaration>().newRequest(target)
            request.search().all {ref -> KotlinFindUsagesHandler.processUsage(processor, ref)}
        }!!
    }

    public override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return getFactory().defaultOptions
    }
}
