/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.tasks

import com.google.common.base.Predicate
import com.google.common.collect.Collections2
import com.google.common.collect.Lists
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetReferenceExpression
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.jet.storage.StorageManager

public class ResolutionTaskHolder<D : CallableDescriptor, F : D>(
        private val storageManager: StorageManager,
        private val basicCallResolutionContext: BasicCallResolutionContext,
        private val priorityProvider: ResolutionTaskHolder.PriorityProvider<ResolutionCandidate<D>>,
        private val tracing: TracingStrategy
) {
    private val isSafeCall: Boolean

    private val candidatesList = Lists.newArrayList<Collection<ResolutionCandidate<D>>>()

    private var internalTasks: MutableList<ResolutionTask<D, F>>? = null

    {
        this.isSafeCall = JetPsiUtil.isSafeCall(basicCallResolutionContext.call)
    }

    public fun setIsSafeCall(candidates: Collection<ResolutionCandidate<D>>): Collection<ResolutionCandidate<D>> {
        for (candidate in candidates) {
            candidate.setSafeCall(isSafeCall)
        }
        return candidates
    }

    public fun addCandidates(candidates: Collection<ResolutionCandidate<D>>) {
        assertNotFinished()
        if (!candidates.isEmpty()) {
            candidatesList.add(setIsSafeCall(candidates))
        }
    }

    public fun addCandidates(candidatesList: List<Collection<ResolutionCandidate<D>>>) {
        assertNotFinished()
        for (candidates in candidatesList) {
            addCandidates(candidates)
        }
    }

    private fun assertNotFinished() {
        assert(internalTasks == null, "Can't add candidates after the resulting tasks were computed.")
    }

    public fun getTasks(): List<ResolutionTask<D, F>> {
        if (internalTasks == null) {
            internalTasks = Lists.newArrayList()

            run {
                var priority = priorityProvider.getMaxPriority()
                while (priority >= 0) {
                    for (candidates in candidatesList) {
                        val filteredCandidates = candidates.filter { priority == priorityProvider.getPriority(it) }
                        if (!filteredCandidates.isEmpty()) {
                            internalTasks!!.add(ResolutionTask(filteredCandidates, basicCallResolutionContext, tracing))
                        }
                    }
                    priority--
                }
            }
        }
        return internalTasks!!
    }

    public trait PriorityProvider<D> {
        public fun getPriority(candidate: D): Int

        public fun getMaxPriority(): Int
    }
}
