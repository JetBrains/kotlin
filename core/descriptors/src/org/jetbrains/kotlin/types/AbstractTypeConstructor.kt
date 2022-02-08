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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.refineTypes

abstract class AbstractTypeConstructor(storageManager: StorageManager) : ClassifierBasedTypeConstructor() {
    override fun getSupertypes() = supertypes().supertypesWithoutCycles

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeConstructor = ModuleViewTypeConstructor(kotlinTypeRefiner)

    @TypeRefinement
    private inner class ModuleViewTypeConstructor(
        private val kotlinTypeRefiner: KotlinTypeRefiner
    ) : TypeConstructor {
        /* NB: it is important to use PUBLICATION here instead of 'storageManager.createLazyValue { ... }'

        The reason is that 'storageManager' can be a storage manager from DefaultBuiltIns (e.g. is this type constructor
        is type constructor of some built-in class like 'Int'). Therefore, call to refined supertypes would result in
        the following order of acquiring locks: DefaultBuiltIns lock -> Sources lock

        Obviously, a lot of code acquires locks in different order (sources lock first, then built-ins lock), so that would
        result in deadlock
         */
        private val refinedSupertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
            @OptIn(TypeRefinement::class)
            kotlinTypeRefiner.refineTypes(this@AbstractTypeConstructor.getSupertypes())
        }

        override fun getParameters(): List<TypeParameterDescriptor> = this@AbstractTypeConstructor.parameters

        override fun getSupertypes(): List<KotlinType> = refinedSupertypes

        override fun isFinal(): Boolean = this@AbstractTypeConstructor.isFinal
        override fun isDenotable(): Boolean = this@AbstractTypeConstructor.isDenotable

        override fun getDeclarationDescriptor() = this@AbstractTypeConstructor.declarationDescriptor

        override fun getBuiltIns(): KotlinBuiltIns = this@AbstractTypeConstructor.builtIns

        override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeConstructor =
            this@AbstractTypeConstructor.refine(kotlinTypeRefiner)

        override fun equals(other: Any?) = this@AbstractTypeConstructor.equals(other)
        override fun hashCode() = this@AbstractTypeConstructor.hashCode()
        override fun toString() = this@AbstractTypeConstructor.toString()
    }

    // In current version diagnostic about loops in supertypes is reported on each vertex (supertype reference) that lies on the cycle.
    // To achieve that we store both versions of supertypes --- before and after loops disconnection.
    // The first one is used for computation of neighbours in supertypes graph (see Companion.computeNeighbours)
    private class Supertypes(val allSupertypes: Collection<KotlinType>) {
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
                    { it.computeNeighbours(useCompanions = false) },
                    { reportSupertypeLoopError(it) }
                )

            if (resultWithoutCycles.isEmpty()) {
                resultWithoutCycles = defaultSupertypeIfEmpty()?.let { listOf(it) }.orEmpty()
            }

            // We also check if there are a loop with additional edges going from owner of companion to
            // the companion itself.
            // Note that we use already disconnected types to not report two diagnostics on cyclic supertypes
            if (shouldReportCyclicScopeWithCompanionWarning) {
                supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
                    this, resultWithoutCycles,
                    { it.computeNeighbours(useCompanions = true) },
                    { reportScopesLoopError(it) }
                )
            }

            supertypes.supertypesWithoutCycles =
                processSupertypesWithoutCycles(resultWithoutCycles as? List<KotlinType> ?: resultWithoutCycles.toList())
        })

    private fun TypeConstructor.computeNeighbours(useCompanions: Boolean): Collection<KotlinType> =
        (this as? AbstractTypeConstructor)?.let { abstractClassifierDescriptor ->
            abstractClassifierDescriptor.supertypes().allSupertypes +
                    abstractClassifierDescriptor.getAdditionalNeighboursInSupertypeGraph(useCompanions)
        } ?: supertypes

    protected abstract fun computeSupertypes(): Collection<KotlinType>
    protected abstract val supertypeLoopChecker: SupertypeLoopChecker
    protected open fun reportSupertypeLoopError(type: KotlinType) {}

    protected open fun processSupertypesWithoutCycles(supertypes: List<@JvmSuppressWildcards KotlinType>): List<KotlinType> = supertypes

    // TODO: overload in AbstractTypeParameterDescriptor?
    protected open fun reportScopesLoopError(type: KotlinType) {}
    protected open val shouldReportCyclicScopeWithCompanionWarning: Boolean = false

    protected open fun getAdditionalNeighboursInSupertypeGraph(useCompanions: Boolean): Collection<KotlinType> = emptyList()
    protected open fun defaultSupertypeIfEmpty(): KotlinType? = null

    // Only for debugging
    fun renderAdditionalDebugInformation(): String = "supertypes=${supertypes.renderDebugInformation()}"

}
