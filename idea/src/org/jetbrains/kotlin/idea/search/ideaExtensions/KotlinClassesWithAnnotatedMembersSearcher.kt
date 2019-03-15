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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassesWithAnnotatedMembersSearch
import com.intellij.psi.search.searches.ScopedQueryExecutor
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.compatibility.ExecutorProcessor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class KotlinClassesWithAnnotatedMembersSearcher : ScopedQueryExecutor<PsiClass, ClassesWithAnnotatedMembersSearch.Parameters> {
    override fun getScope(param: ClassesWithAnnotatedMembersSearch.Parameters): GlobalSearchScope {
        return GlobalSearchScope.getScopeRestrictedByFileTypes(param.annotationClass.project.allScope(), KotlinFileType.INSTANCE)
    }

    override fun execute(queryParameters: ClassesWithAnnotatedMembersSearch.Parameters, consumer: ExecutorProcessor<PsiClass>): Boolean {
        val processed = hashSetOf<KtClassOrObject>()
        return KotlinAnnotatedElementsSearcher.processAnnotatedMembers(queryParameters.annotationClass,
                                                                       queryParameters.scope,
                                                                       { it.getNonStrictParentOfType<KtClassOrObject>() !in processed }) { declaration ->
            val ktClass = declaration.getNonStrictParentOfType<KtClassOrObject>()
            if (ktClass != null && processed.add(ktClass)) {
                val lightClass = ktClass.toLightClass()
                if (lightClass != null) consumer.process(lightClass) else true
            } else
                true
        }
    }
}
