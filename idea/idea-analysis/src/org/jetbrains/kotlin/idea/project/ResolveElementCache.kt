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
import org.jetbrains.kotlin.asJava.KotlinCodeBlockModificationListener
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.SubtreeModificationCountUpdater
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingPropertyShortNameIndex
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BodyResolveCache
import org.jetbrains.kotlin.resolve.StatementFilter
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.lazy.*

public class ResolveElementCache(resolveSession: ResolveSession, private val project: Project) : ElementResolver(resolveSession), BodyResolveCache {
    private class CachedResolve(val bindingContext: BindingContext, resolveElement: JetElement) {
        private val elementModificationCount: Int? =
                if (resolveElement is JetDeclaration && KotlinCodeBlockModificationListener.isBlockDeclaration(resolveElement))
                    SubtreeModificationCountUpdater.getModificationCount(resolveElement)
                else
                    null

        fun isUpToDate(resolveElement: JetElement)
                = elementModificationCount == null || elementModificationCount == SubtreeModificationCountUpdater.getModificationCount(resolveElement)
    }

    // drop whole cache after change "out of code block"
    private val fullResolveCache: CachedValue<MutableMap<JetElement, CachedResolve>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MutableMap<JetElement, CachedResolve>> {
                override fun compute(): CachedValueProvider.Result<MutableMap<JetElement, CachedResolve>> {
                    return CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<JetElement, CachedResolve>(),
                                                             PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                                                             resolveSession.exceptionTracker)
                }
            },
            false)

    private val partialBodyResolveCache: CachedValue<MutableMap<JetExpression, BindingContext>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MutableMap<JetExpression, BindingContext>> {
                override fun compute(): CachedValueProvider.Result<MutableMap<JetExpression, BindingContext>> {
                    return CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<JetExpression, BindingContext>(),
                                                             PsiModificationTracker.MODIFICATION_COUNT,
                                                             resolveSession.exceptionTracker)
                }
            },
            false)

    override fun getElementAdditionalResolve(resolveElement: JetElement, contextElement: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        // check if full additional resolve already performed and is up-to-date
        val fullResolveMap = fullResolveCache.value
        val cachedFullResolve = fullResolveMap[resolveElement]
        if (cachedFullResolve != null) {
            if (cachedFullResolve.isUpToDate(resolveElement)) {
                return cachedFullResolve.bindingContext
            }
            else {
                fullResolveMap.remove(resolveElement) // remove outdated cache entry
            }
        }

        when (bodyResolveMode) {
            BodyResolveMode.FULL -> {
                val bindingContext = performElementAdditionalResolve(resolveElement, resolveElement, BodyResolveMode.FULL).first
                fullResolveMap[resolveElement] = CachedResolve(bindingContext, resolveElement)
                return bindingContext
            }

            BodyResolveMode.PARTIAL -> {
                if (resolveElement !is JetDeclaration) {
                    return getElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.FULL)
                }

                val statementToResolve = PartialBodyResolveFilter.findStatementToResolve(contextElement, resolveElement)
                val partialResolveMap = partialBodyResolveCache.value
                partialResolveMap[statementToResolve ?: resolveElement]?.let { return it } // partial resolve is already cached for this statement

                val (bindingContext, statementFilter) = performElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.PARTIAL)

                if (statementFilter == StatementFilter.NONE) { // partial resolve is not supported for the given declaration - full resolve performed instead
                    fullResolveMap[resolveElement] = CachedResolve(bindingContext, resolveElement)
                    return bindingContext
                }

                for (statement in (statementFilter as PartialBodyResolveFilter).allStatementsToResolve) {
                    if (!partialResolveMap.containsKey(statement) && bindingContext[BindingContext.PROCESSED, statement] == true) {
                        partialResolveMap[statement] = bindingContext
                    }
                }
                partialResolveMap[resolveElement] = bindingContext // we use the whole declaration key in the map to obtain resolve not inside any block (e.g. default parameter values)

                return bindingContext
            }

            BodyResolveMode.PARTIAL_FOR_COMPLETION -> {
                if (resolveElement !is JetDeclaration) {
                    return getElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.FULL)
                }

                // not cached
                return performElementAdditionalResolve(resolveElement, contextElement, bodyResolveMode).first
            }
        }
    }

    override fun getTargetPlatform(file: JetFile): TargetPlatform = TargetPlatformDetector.getPlatform(file)

    override fun probablyNothingCallableNames(): ProbablyNothingCallableNames {
        return object : ProbablyNothingCallableNames {
            override fun functionNames() = JetProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project)
            override fun propertyNames() = JetProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project)
        }
    }

    override fun resolveFunctionBody(function: JetNamedFunction)
            = getElementAdditionalResolve(function, function, BodyResolveMode.FULL)
}
