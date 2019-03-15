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
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import org.jetbrains.kotlin.asJava.toLightClassWithBuiltinMapping
import org.jetbrains.kotlin.compatibility.ExecutorProcessor
import org.jetbrains.kotlin.idea.caches.lightClasses.KtFakeLightClass
import org.jetbrains.kotlin.idea.search.fileScope
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.KotlinSuperClassIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasByExpansionShortNameIndex
import org.jetbrains.kotlin.idea.util.application.runReadAction

open class KotlinDirectInheritorsSearcher : QueryExecutorBase<PsiClass, DirectClassInheritorsSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: DirectClassInheritorsSearch.SearchParameters, consumer: ExecutorProcessor<PsiClass>) {
        val baseClass = queryParameters.classToProcess

        val name = baseClass.name ?: return

        val file = if (baseClass is KtFakeLightClass) baseClass.kotlinOrigin.containingFile else baseClass.containingFile

        val originalScope = queryParameters.scope
        val scope = originalScope as? GlobalSearchScope ?: file.fileScope() ?: return

        val names = mutableSetOf(name)
        val project = file.project

        val typeAliasIndex = KotlinTypeAliasByExpansionShortNameIndex.getInstance()

        fun searchForTypeAliasesRecursively(typeName: String) {
            ProgressManager.checkCanceled()
            typeAliasIndex[typeName, project, scope].asSequence()
                .map { it.name }
                .filterNotNull()
                .filter { it !in names }
                .onEach { names.add(it) }
                .forEach(::searchForTypeAliasesRecursively)
        }

        searchForTypeAliasesRecursively(name)

        runReadAction {
            val noLibrarySourceScope = KotlinSourceFilterScope.projectSourceAndClassFiles(scope, baseClass.project)

            names.forEach { name ->
                KotlinSuperClassIndex.getInstance()
                    .get(name, baseClass.project, noLibrarySourceScope).asSequence()
                    .mapNotNull { candidate -> candidate.toLightClassWithBuiltinMapping() ?: KtFakeLightClass(candidate) }
                    .filter { candidate -> candidate.isInheritor(baseClass, false) }
                    .forEach { candidate -> consumer.process(candidate) }
            }
        }
    }
}
