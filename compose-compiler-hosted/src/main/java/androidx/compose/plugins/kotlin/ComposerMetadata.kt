/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.model.DataFlowInfoForArgumentsImpl
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isNothingOrNullableNothing
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class ComposerMetadata(
    val type: KotlinType,
    // Set of valid upper bound types that were defined on the composer that can't have children
    // For android, this should be [View]
    private val emitSimpleUpperBoundTypes: Set<KotlinType>,
    // Set of valid upper bound types that were defined on the composer that can have children.
    // For android, this would be [ViewGroup]
    private val emitCompoundUpperBoundTypes: Set<KotlinType>,
    // The specification for `emit` on a composer allows for the `ctor` parameter to be a function type
    // with any number of parameters. We allow for these parameters to be used as parameters in the
    // Constructors that are emitted with a KTX tag. These parameters can be overridden with attributes
    // in the KTX tag, but if there are required parameters with a type that matches one declared in the
    // ctor parameter, we will resolve it automatically with the value passed in the `ctor` lambda.
    //
    // In order to do this resolution, we store a list of pairs of "upper bounds" to parameter types. For example,
    // the following emit call:
    //
    //      fun <T : View> emit(key: Any, ctor: (context: Context) -> T, update: U<T>.() -> Unit)
    //
    // would produce a Pair of [View] to [Context]
    private val emittableTypeToImplicitCtorTypes: List<Pair<List<KotlinType>, Set<KotlinType>>>
) {

    companion object {
        private fun resolveComposerMethodCandidates(
            name: Name,
            context: BasicCallResolutionContext,
            composerType: KotlinType,
            callResolver: CallResolver,
            psiFactory: KtPsiFactory
        ): Collection<ResolvedCall<*>> {
            val calleeExpression = psiFactory.createSimpleName(name.asString())

            val methodCall = makeCall(
                callElement = context.call.callElement,
                calleeExpression = calleeExpression,
                receiver = TransientReceiver(
                    composerType
                )
            )

            val contextForVariable =
                BasicCallResolutionContext.create(
                    context,
                    methodCall,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    DataFlowInfoForArgumentsImpl(
                        context.dataFlowInfo,
                        methodCall
                    )
                )

            val results = callResolver.resolveCallWithGivenName(
                // it's important that we use "collectAllCandidates" so that extension functions get included
                contextForVariable.replaceCollectAllCandidates(true),
                methodCall,
                calleeExpression,
                name
            )

            return results.allCandidates ?: emptyList()
        }

        fun build(
            composerType: KotlinType,
            callResolver: CallResolver,
            psiFactory: KtPsiFactory,
            resolutionContext: BasicCallResolutionContext
        ): ComposerMetadata {
            val emitSimpleUpperBoundTypes = mutableSetOf<KotlinType>()
            val emitCompoundUpperBoundTypes = mutableSetOf<KotlinType>()
            val emittableTypeToImplicitCtorTypes =
                mutableListOf<Pair<List<KotlinType>, Set<KotlinType>>>()

            val emitCandidates = resolveComposerMethodCandidates(
                KtxNameConventions.EMIT,
                resolutionContext,
                composerType,
                callResolver,
                psiFactory
            )

            for (candidate in emitCandidates.map { it.candidateDescriptor }) {
                if (candidate.name != KtxNameConventions.EMIT) continue
                if (candidate !is SimpleFunctionDescriptor) continue
                val params = candidate.valueParameters
                // NOTE(lmr): we could report diagnostics on some of these? it seems strange to emit diagnostics about a function
                // that is not necessarily being used though. I think it's probably better to just ignore them here.

                // the signature of emit that we are looking for has 3 or 4 parameters
                if (params.size < 3 || params.size > 4) continue
                val ctorParam = params.find {
                    it.name == KtxNameConventions.EMIT_CTOR_PARAMETER
                } ?: continue
                if (!ctorParam.type.isFunctionTypeOrSubtype) continue

                // the return type from the ctor param is the "upper bound" of the node type. It will often be a generic type with constraints.
                val upperBounds = ctorParam.type.getReturnTypeFromFunctionType().upperBounds()

                // the ctor param can have parameters itself, which we interpret as implicit parameter types that the composer knows how to
                // automatically provide to the component. In the case of Android Views, this is how we automatically provide Context.
                val implicitParamTypes =
                    ctorParam.type.getValueParameterTypesFromFunctionType().map {
                        it.type
                    }

                for (implicitType in implicitParamTypes) {
                    emittableTypeToImplicitCtorTypes.add(upperBounds to implicitParamTypes.toSet())
                }

                emitSimpleUpperBoundTypes.addAll(upperBounds)

                if (params.any { it.name == KtxNameConventions.EMIT_CHILDREN_PARAMETER }) {
                    emitCompoundUpperBoundTypes.addAll(upperBounds)
                }
            }

            return ComposerMetadata(
                composerType,
                emitSimpleUpperBoundTypes,
                emitCompoundUpperBoundTypes,
                emittableTypeToImplicitCtorTypes
            )
        }

        fun getOrBuild(
            composerType: KotlinType,
            callResolver: CallResolver,
            psiFactory: KtPsiFactory,
            resolutionContext: BasicCallResolutionContext
        ): ComposerMetadata {
            val meta = resolutionContext.trace.bindingContext[
                    ComposeWritableSlices.COMPOSER_METADATA,
                    composerType
            ]
            return if (meta == null) {
                val built = build(composerType, callResolver, psiFactory, resolutionContext)
                resolutionContext.trace.record(
                    ComposeWritableSlices.COMPOSER_METADATA,
                    composerType,
                    built
                )
                built
            } else {
                meta
            }
        }
    }

    fun isEmittable(type: KotlinType) =
        !type.isError && !type.isNothingOrNullableNothing() && emitSimpleUpperBoundTypes.any {
            type.isSubtypeOf(it)
        }

    fun isCompoundEmittable(type: KotlinType) = !type.isError &&
            !type.isNothingOrNullableNothing() &&
            emitCompoundUpperBoundTypes.any {
                type.isSubtypeOf(it)
            }

    fun isImplicitConstructorParam(
        param: ValueParameterDescriptor,
        fn: CallableDescriptor
    ): Boolean {
        val returnType = fn.returnType ?: return false
        val paramType = param.type
        for ((upperBounds, implicitTypes) in emittableTypeToImplicitCtorTypes) {
            if (!implicitTypes.any { it.isSubtypeOf(paramType) }) continue
            if (!returnType.satisfiesConstraintsOf(upperBounds)) continue
            return true
        }
        return false
    }
}