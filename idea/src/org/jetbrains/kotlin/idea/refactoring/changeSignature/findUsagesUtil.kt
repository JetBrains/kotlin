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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiReference
import com.intellij.usageView.UsageInfo
import com.intellij.util.CommonProcessors
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.psi.JetParameter

// searching parameter usages via ReferenceSearch is currently too slow (uses non-optimized search for named arguments),
// that's why we use KotlinFindUsagesHandler
fun findParameterUsages(parameter: JetParameter): Collection<PsiReference> {
    val project = parameter.project
    val findUsagesHandler = KotlinFindUsagesHandlerFactory(project).createFindUsagesHandlerNoQuestions(parameter)
    val processor = CommonProcessors.CollectProcessor<UsageInfo>()
    val options = KotlinPropertyFindUsagesOptions(project)
    findUsagesHandler.processElementUsages(parameter, processor, options)
    return processor.results.map { it.reference }.filterNotNull()
}