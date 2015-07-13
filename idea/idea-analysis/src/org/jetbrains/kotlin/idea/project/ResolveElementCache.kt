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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BodyResolveCache
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull

public class ResolveElementCache(resolveSession: ResolveSession, private val project: Project) : ElementResolver(resolveSession), BodyResolveCache {
    // Recreate internal cache after change of modification count
    private val additionalResolveCache: CachedValue<MemoizedFunctionToNotNull<JetElement, BindingContext>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MemoizedFunctionToNotNull<JetElement, BindingContext>> {
                override fun compute(): CachedValueProvider.Result<MemoizedFunctionToNotNull<JetElement, BindingContext>> {
                    val manager = resolveSession.getStorageManager()
                    val cacheFunction = manager.createSoftlyRetainedMemoizedFunction<JetElement, BindingContext> { element ->
                        performElementAdditionalResolve(element, element, BodyResolveMode.FULL)
                    }
                    return CachedValueProvider.Result.create(cacheFunction,
                                                             PsiModificationTracker.MODIFICATION_COUNT,
                                                             resolveSession.getExceptionTracker())
                }
            },
            false)

    private val partialBodyResolveCache: CachedValue<MemoizedFunctionToNotNull<JetExpression, BindingContext>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MemoizedFunctionToNotNull<JetExpression, BindingContext>> {
                override fun compute(): CachedValueProvider.Result<MemoizedFunctionToNotNull<JetExpression, BindingContext>> {
                    val manager = resolveSession.getStorageManager()
                    val cacheFunction = manager.createSoftlyRetainedMemoizedFunction<JetExpression, BindingContext> { expression ->
                        val resolveElement = findElementOfAdditionalResolve(expression)!!
                        performElementAdditionalResolve(resolveElement, expression, BodyResolveMode.PARTIAL)
                    }
                    return CachedValueProvider.Result.create(cacheFunction,
                                                             PsiModificationTracker.MODIFICATION_COUNT,
                                                             resolveSession.getExceptionTracker())
                }
            },
            false)

    override fun getElementAdditionalResolve(resolveElement: JetElement, contextElement: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        if (bodyResolveMode != BodyResolveMode.FULL && !hasElementAdditionalResolveCached(resolveElement) && resolveElement is JetDeclaration) {
            if (bodyResolveMode == BodyResolveMode.PARTIAL) {
                val partialResolveElement = PartialBodyResolveFilter.findResolveElement(contextElement, resolveElement)
                if (partialResolveElement != null) {
                    return partialBodyResolveCache.getValue().invoke(partialResolveElement)
                }
            }
            else {
                return performElementAdditionalResolve(resolveElement, contextElement, bodyResolveMode)
            }
        }

        return additionalResolveCache.getValue().invoke(resolveElement)
    }

    private fun hasElementAdditionalResolveCached(jetElement: JetElement)
            = additionalResolveCache.hasUpToDateValue() && additionalResolveCache.getValue().isComputed(jetElement)

    override fun createAdditionalCheckerProvider(jetFile: JetFile, module: ModuleDescriptor)
            = TargetPlatformDetector.getPlatform(jetFile).createAdditionalCheckerProvider(module)

    override fun getDynamicTypesSettings(jetFile: JetFile)
            = TargetPlatformDetector.getPlatform(jetFile).getDynamicTypesSettings()

    override fun probablyNothingCallableNames(): ProbablyNothingCallableNames {
        return object : ProbablyNothingCallableNames {
            override fun functionNames() = JetProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project)
            override fun propertyNames() = JetProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project)
        }
    }

    override fun resolveFunctionBody(function: JetNamedFunction)
            = getElementAdditionalResolve(function, function, BodyResolveMode.FULL)
}
