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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.decompiler.navigation.SourceNavigationHelper
import org.jetbrains.kotlin.idea.search.fileScope
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.KotlinSuperClassIndex
import org.jetbrains.kotlin.idea.util.application.runReadAction

open class KotlinDirectInheritorsSearcher() : QueryExecutorBase<PsiClass, DirectClassInheritorsSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: DirectClassInheritorsSearch.SearchParameters, consumer: Processor<PsiClass>) {
        val baseClass = queryParameters.classToProcess
        if (baseClass == null) return

        val name = baseClass.name
        if (name == null) return

        val originalScope = queryParameters.scope
        val scope = if (originalScope is GlobalSearchScope)
            originalScope
        else
            baseClass.containingFile?.let { file -> file.fileScope() }

        if (scope == null) return

        runReadAction {
            val noLibrarySourceScope = KotlinSourceFilterScope.projectSourceAndClassFiles(scope, baseClass.project)
            KotlinSuperClassIndex.getInstance()
                    .get(name, baseClass.project, noLibrarySourceScope).asSequence()
                    .mapNotNull { candidate -> SourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(candidate) }
                    .filter { candidate -> candidate.isInheritor(baseClass, false) }
                    .forEach { candidate -> consumer.process(candidate) }
        }
    }
}
