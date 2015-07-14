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
import com.intellij.util.containers.ContainerUtil
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
                        performElementAdditionalResolve(element, element, BodyResolveMode.FULL).first
                    }
                    return CachedValueProvider.Result.create(cacheFunction,
                                                             PsiModificationTracker.MODIFICATION_COUNT,
                                                             resolveSession.getExceptionTracker())
                }
            },
            false)

    private val partialBodyResolveCache: CachedValue<MutableMap<JetExpression, BindingContext>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MutableMap<JetExpression, BindingContext>> {
                override fun compute(): CachedValueProvider.Result<MutableMap<JetExpression, BindingContext>> {
                    return CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<JetExpression, BindingContext>(),
                                                             PsiModificationTracker.MODIFICATION_COUNT,
                                                             resolveSession.getExceptionTracker())
                }
            },
            false)

    override fun getElementAdditionalResolve(resolveElement: JetElement, contextElement: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        if (bodyResolveMode != BodyResolveMode.FULL && !hasElementAdditionalResolveCached(resolveElement) && resolveElement is JetDeclaration) {
            if (bodyResolveMode == BodyResolveMode.PARTIAL) {
                val statementToResolve = PartialBodyResolveFilter.findStatementToResolve(contextElement, resolveElement)
                val map = partialBodyResolveCache.getValue()
                map[statementToResolve ?: resolveElement]?.let { return it }

                val (bindingContext, statementFilter) = performElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.PARTIAL)

                if (statementFilter is PartialBodyResolveFilter) {
                    for (statement in statementFilter.allStatementsToResolve) {
                        if (!map.containsKey(statement) && bindingContext[BindingContext.PROCESSED, statement] == true) {
                            map[statement] = bindingContext
                        }
                    }
                }
                map[resolveElement] = bindingContext // we use the whole declaration key in the map to obtain resolve not inside any block (e.g. default parameter values)

                return bindingContext
            }
            else {
                return performElementAdditionalResolve(resolveElement, contextElement, bodyResolveMode).first
            }
        }

        return additionalResolveCache.getValue().invoke(resolveElement)
    }

    private fun hasElementAdditionalResolveCached(element: JetElement)
            = additionalResolveCache.hasUpToDateValue() && additionalResolveCache.getValue().isComputed(element)

    override fun createAdditionalCheckerProvider(file: JetFile, module: ModuleDescriptor)
            = TargetPlatformDetector.getPlatform(file).createAdditionalCheckerProvider(module)

    override fun getDynamicTypesSettings(file: JetFile)
            = TargetPlatformDetector.getPlatform(file).getDynamicTypesSettings()

    override fun probablyNothingCallableNames(): ProbablyNothingCallableNames {
        return object : ProbablyNothingCallableNames {
            override fun functionNames() = JetProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project)
            override fun propertyNames() = JetProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project)
        }
    }

    override fun resolveFunctionBody(function: JetNamedFunction)
            = getElementAdditionalResolve(function, function, BodyResolveMode.FULL)
}
