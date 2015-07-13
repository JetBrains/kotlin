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

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingPropertyShortNameIndex
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BodyResolveCache
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ElementResolver
import org.jetbrains.kotlin.resolve.lazy.ProbablyNothingCallableNames
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.storage.LazyResolveStorageManager
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull
import org.jetbrains.kotlin.types.DynamicTypesSettings

public class ResolveElementCache(resolveSession: ResolveSession, private val project: Project) : ElementResolver(resolveSession), BodyResolveCache {
    private val additionalResolveCache: CachedValue<MemoizedFunctionToNotNull<JetElement, BindingContext>>

    init {

        // Recreate internal cache after change of modification count
        this.additionalResolveCache = CachedValuesManager.getManager(project).createCachedValue(object : CachedValueProvider<MemoizedFunctionToNotNull<JetElement, BindingContext>> {
            override fun compute(): CachedValueProvider.Result<MemoizedFunctionToNotNull<JetElement, BindingContext>>? {
                val resolveSession = this@ResolveElementCache.resolveSession
                val manager = resolveSession.getStorageManager()
                val elementsCacheFunction = manager.createSoftlyRetainedMemoizedFunction(object : Function1<JetElement, BindingContext> {
                    override fun invoke(jetElement: JetElement): BindingContext {
                        return performElementAdditionalResolve(jetElement, jetElement, BodyResolveMode.FULL)
                    }
                })

                return CachedValueProvider.Result.create(elementsCacheFunction,
                                                         PsiModificationTracker.MODIFICATION_COUNT,
                                                         resolveSession.getExceptionTracker())
            }
        },
                                                                                                false)
    }

    override fun getElementAdditionalResolve(jetElement: JetElement): BindingContext {
        return additionalResolveCache.getValue().invoke(jetElement)
    }

    override fun hasElementAdditionalResolveCached(jetElement: JetElement): Boolean {
        if (!additionalResolveCache.hasUpToDateValue()) return false
        return additionalResolveCache.getValue().isComputed(jetElement)
    }

    override fun createAdditionalCheckerProvider(jetFile: JetFile, module: ModuleDescriptor): AdditionalCheckerProvider {
        return TargetPlatformDetector.getPlatform(jetFile).createAdditionalCheckerProvider(module)
    }

    override fun getDynamicTypesSettings(jetFile: JetFile): DynamicTypesSettings {
        return TargetPlatformDetector.getPlatform(jetFile).getDynamicTypesSettings()
    }

    override fun probablyNothingCallableNames(): ProbablyNothingCallableNames {
        return object : ProbablyNothingCallableNames {
            override fun functionNames(): Collection<String> {
                // we have to add hardcoded-names until we have Kotlin compiled classes in caches
                return JetProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project)
            }

            override fun propertyNames(): Collection<String> {
                return JetProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project)
            }
        }
    }

    override fun resolveFunctionBody(function: JetNamedFunction): BindingContext {
        return getElementAdditionalResolve(function)
    }
}
