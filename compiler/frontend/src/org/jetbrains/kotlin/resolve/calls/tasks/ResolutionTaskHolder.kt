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

package org.jetbrains.kotlin.resolve.calls.tasks

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.storage.StorageManager
import java.util.ArrayList
import org.jetbrains.kotlin.utils.toReadOnlyList

public class ResolutionTaskHolder<D : CallableDescriptor, F : D>(
        private val storageManager: StorageManager,
        private val basicCallResolutionContext: BasicCallResolutionContext,
        private val priorityProvider: ResolutionTaskHolder.PriorityProvider<ResolutionCandidate<D>>,
        private val tracing: TracingStrategy
) {
    private val candidatesList = ArrayList<() -> Collection<ResolutionCandidate<D>>>()
    private var internalTasks: List<ResolutionTask<D, F>>? = null

    public fun addCandidates(lazyCandidates: () -> Collection<ResolutionCandidate<D>>) {
        assertNotFinished()
        candidatesList.add(storageManager.createLazyValue { lazyCandidates().toReadOnlyList() })
    }

    public fun addCandidates(candidatesList: List<Collection<ResolutionCandidate<D>>>) {
        assertNotFinished()
        for (candidates in candidatesList) {
            addCandidates { candidates }
        }
    }

    private fun assertNotFinished() {
        assert(internalTasks == null, "Can't add candidates after the resulting tasks were computed.")
    }

    public fun getTasks(): List<ResolutionTask<D, F>> {
        if (internalTasks == null) {
            val tasks = ArrayList<ResolutionTask<D, F>>()
            for (priority in (0..priorityProvider.getMaxPriority()).reversed()) {
                for (candidateIndex in candidatesList.indices) {
                    val lazyCandidates = {
                        candidatesList[candidateIndex]().filter { priorityProvider.getPriority(it) == priority }.toReadOnlyList()
                    }
                    tasks.add(ResolutionTask(basicCallResolutionContext, tracing, lazyCandidates))
                }
            }

            internalTasks = tasks
        }
        return internalTasks!!
    }

    public trait PriorityProvider<D> {
        public fun getPriority(candidate: D): Int

        public fun getMaxPriority(): Int
    }
}
