/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.inference.components.SimpleConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallContext
import org.jetbrains.kotlin.resolve.calls.model.CallableReferenceKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature
import org.jetbrains.kotlin.resolve.calls.results.OverloadingConflictResolver
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.tower.TowerResolver
import org.jetbrains.kotlin.types.UnwrappedType


class CallableReferenceOverloadConflictResolver(
        builtIns: KotlinBuiltIns,
        specificityComparator: TypeSpecificityComparator,
        externalPredicates: KotlinResolutionExternalPredicates,
        constraintInjector: ConstraintInjector,
        typeResolver: ResultTypeResolver
) : OverloadingConflictResolver<CallableReferenceCandidate>(
        builtIns,
        specificityComparator,
        { it.candidate },
        { SimpleConstraintSystemImpl(constraintInjector, typeResolver) },
        Companion::createFlatSignature,
        { null },
        { externalPredicates.isDescriptorFromSource(it) }
        ) {
    companion object {
        private fun createFlatSignature(candidate: CallableReferenceCandidate) =
                FlatSignature.createFromReflectionType(candidate, candidate.candidate, candidate.numDefaults, candidate.reflectionCandidateType)
    }
}

class PostponeCallableReferenceArgument(
        val argument: CallableReferenceKotlinCallArgument,
        val expectedType: UnwrappedType
)

class CallableReferenceResolver(
        val towerResolver: TowerResolver,
        val callableReferenceOverloadConflictResolver: CallableReferenceOverloadConflictResolver
) {

    fun runRLSResolution(
            outerCallContext: KotlinCallContext,
            callableReference: CallableReferenceKotlinCallArgument,
            expectedType: UnwrappedType?, // this type can have not fixed type variable inside
            compatibilityChecker: ((ConstraintSystemOperation) -> Unit) -> Unit // you can run anything throw this operation and all this operation will be roll backed
    ): Set<CallableReferenceCandidate> {
        val factory = CallableReferencesCandidateFactory(callableReference, outerCallContext, compatibilityChecker, expectedType)
        val processor = createCallableReferenceProcessor(factory)
        val candidates = towerResolver.runResolve(outerCallContext.scopeTower, processor, useOrder = true)
        return callableReferenceOverloadConflictResolver.chooseMaximallySpecificCandidates(candidates, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                                                                   discriminateGenerics = false,
                                                                   isDebuggerContext = outerCallContext.scopeTower.isDebuggerContext)
    }
}

