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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.storage.StorageManager

abstract class AbstractTypeConstructor(storageManager: StorageManager) : TypeConstructor {
    override fun getSupertypes() = supertypes().supertypesWithoutCycles

    // In current version diagnostic about loops in supertypes is reported on each vertex (supertype reference) that lies on the cycle.
    // To achieve that we store both versions of supertypes --- before and after loops disconnection.
    // The first one is used for computation of neighbours in supertypes graph (see Companion.computeNeighbours)
    private class Supertypes(
            val allSupertypes: Collection<KotlinType>) {
            // initializer is only needed as a stub for case when 'getSupertypes' is called while 'supertypes' are being calculated
            var supertypesWithoutCycles: List<KotlinType> = listOf(ErrorUtils.ERROR_TYPE_FOR_LOOP_IN_SUPERTYPES)
    }

    private val supertypes = storageManager.createLazyValueWithPostCompute(
            { Supertypes(computeSupertypes()) },
            { Supertypes(listOf(ErrorUtils.ERROR_TYPE_FOR_LOOP_IN_SUPERTYPES)) },
            { supertypes ->
                // It's important that loops disconnection begins in post-compute phase, because it guarantees that
                // when we start calculation supertypes of supertypes (for computing neighbours), they start their disconnection loop process
                // either, and as we want to report diagnostic about loops on all declarations they should see consistent version of 'allSupertypes'
                var resultWithoutCycles =
                        supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
                            this, supertypes.allSupertypes,
                            { it.computeNeighbours() },
                            { reportSupertypeLoopError(it) })

                if (resultWithoutCycles.isEmpty()) {
                    resultWithoutCycles = defaultSupertypeIfEmpty()?.let { listOf(it) }.orEmpty()
                }

                supertypes.supertypesWithoutCycles = (resultWithoutCycles as? List<KotlinType>) ?: resultWithoutCycles.toList()
            })

    private fun TypeConstructor.computeNeighbours(): Collection<KotlinType> =
            (this as? AbstractTypeConstructor)?.let {
                abstractClassifierDescriptor ->
                abstractClassifierDescriptor.supertypes().allSupertypes +
                abstractClassifierDescriptor.getAdditionalNeighboursInSupertypeGraph()
            } ?: supertypes

    protected abstract fun computeSupertypes(): Collection<KotlinType>
    protected abstract val supertypeLoopChecker: SupertypeLoopChecker
    protected open fun reportSupertypeLoopError(type: KotlinType) {}
    protected open fun getAdditionalNeighboursInSupertypeGraph(): Collection<KotlinType> = emptyList()
    protected open fun defaultSupertypeIfEmpty(): KotlinType? = null

}
