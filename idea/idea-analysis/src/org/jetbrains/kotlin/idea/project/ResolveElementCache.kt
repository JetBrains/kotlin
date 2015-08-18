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
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingPropertyShortNameIndex
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BodyResolveCache
import org.jetbrains.kotlin.resolve.StatementFilter
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.utils.addToStdlib.check

public class ResolveElementCache(resolveSession: ResolveSession, private val project: Project) : ElementResolver(resolveSession), BodyResolveCache {
    private class CachedFullResolve(val bindingContext: BindingContext, resolveElement: JetElement) {
        private val modificationStamp: Long? = modificationStamp(resolveElement)

        fun isUpToDate(resolveElement: JetElement) = modificationStamp == modificationStamp(resolveElement)

        private fun modificationStamp(resolveElement: JetElement): Long? {
            val file = resolveElement.containingFile
            return if (!file.isPhysical) // for non-physical file we don't get OUT_OF_CODE_BLOCK_MODIFICATION_COUNT increased and must reset data on any modification of the file
                file.modificationStamp
            else if (resolveElement is JetDeclaration && KotlinCodeBlockModificationListener.isBlockDeclaration(resolveElement))
                resolveElement.getModificationStamp()
            else
                null
        }
    }

    // drop whole cache after change "out of code block"
    private val fullResolveCache: CachedValue<MutableMap<JetElement, CachedFullResolve>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MutableMap<JetElement, CachedFullResolve>> {
                override fun compute(): CachedValueProvider.Result<MutableMap<JetElement, CachedFullResolve>> {
                    return CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<JetElement, CachedFullResolve>(),
                                                             PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                                                             resolveSession.exceptionTracker)
                }
            },
            false)

    private class CachedPartialResolve(val bindingContext: BindingContext, file: JetFile) {
        private val modificationStamp: Long? = modificationStamp(file)

        fun isUpToDate(file: JetFile) = modificationStamp == modificationStamp(file)

        private fun modificationStamp(file: JetFile): Long? {
            return if (!file.isPhysical) // for non-physical file we don't get MODIFICATION_COUNT increased and must reset data on any modification of the file
                file.modificationStamp
            else
                null
        }
    }

    private val partialBodyResolveCache: CachedValue<MutableMap<JetExpression, CachedPartialResolve>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MutableMap<JetExpression, CachedPartialResolve>> {
                override fun compute(): CachedValueProvider.Result<MutableMap<JetExpression, CachedPartialResolve>> {
                    return CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<JetExpression, CachedPartialResolve>(),
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
                fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                return bindingContext
            }

            BodyResolveMode.PARTIAL -> {
                if (resolveElement !is JetDeclaration) {
                    return getElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.FULL)
                }

                val file = resolveElement.getContainingJetFile()
                val statementToResolve = PartialBodyResolveFilter.findStatementToResolve(contextElement, resolveElement)
                val partialResolveMap = partialBodyResolveCache.value
                partialResolveMap[statementToResolve ?: resolveElement]
                        ?.check { it.isUpToDate(file) }
                        ?.let { return it.bindingContext } // partial resolve is already cached for this statement

                val (bindingContext, statementFilter) = performElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.PARTIAL)

                if (statementFilter == StatementFilter.NONE) { // partial resolve is not supported for the given declaration - full resolve performed instead
                    fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                    return bindingContext
                }

                val resolveToCache = CachedPartialResolve(bindingContext, file)

                for (statement in (statementFilter as PartialBodyResolveFilter).allStatementsToResolve) {
                    if (!partialResolveMap.containsKey(statement) && bindingContext[BindingContext.PROCESSED, statement] == true) {
                        partialResolveMap[statement] = resolveToCache
                    }
                }
                partialResolveMap[resolveElement] = resolveToCache // we use the whole declaration key in the map to obtain resolve not inside any block (e.g. default parameter values)

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

