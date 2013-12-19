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

package org.jetbrains.jet.asJava

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValueProvider.Result
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetClassOrObject
import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.psi.JetPsiUtil
import com.intellij.openapi.util.Key
import java.util.HashMap
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetClassInitializer
import org.jetbrains.jet.plugin.project.ResolveElementCache
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValue
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolverUtil

class KotlinLightClassContextCache(val project: Project) {
    private val cacheKey = Key.create<CachedValue<ResolveElementCache>>("KOTLIN_LIGHT_CLASS_CONTEXT_CACHE")

    private val lock = Any()

    private val provider = object: CachedValueProvider<ResolveElementCache> {
        override fun compute(): CachedValueProvider.Result<ResolveElementCache> {
            val trace = DelegatingBindingTrace(
                    KotlinCacheManager.getInstance(project).getPossiblyIncompleteDeclarationsForLightClassGeneration().getBindingContext(),
                    "Trace for KotlinLightClassContextCache"
            )
            val resolveSession = AnalyzerFacadeForJVM.createLazyResolveSession(
                    project,
                    JetFilesProvider.getInstance(project)!!.allInScope(GlobalSearchScope.allScope(project)),
                    trace,
                    InjectorForJavaDescriptorResolverUtil.create(project, trace),
                    true
            )
            return Result.create(
                    ResolveElementCache(resolveSession, project),
                    PsiModificationTracker.MODIFICATION_COUNT,
                    KotlinCacheManager.getInstance(project).getDeclarationsTracker()
            )
        }
    }

    public fun getLightClassContext(classOrObject: JetClassOrObject): BindingContext {
        if (!JetPsiUtil.isLocal(classOrObject)) {
            return KotlinCacheManager.getInstance(project).getPossiblyIncompleteDeclarationsForLightClassGeneration().getBindingContext()
        }

        val resolveElementCache = CachedValuesManager.getManager(project).getCachedValue(project, cacheKey, provider, false)
        return resolveElementCache?.resolveElement(classOrObject) ?: BindingContext.EMPTY
    }

}
