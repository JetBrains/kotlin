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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

// very initial state of component
// todo: handle all diagnostic inside DiagnosticReporterByTrackingStrategy
// move it to frontend module
class AdditionalDiagnosticReporter {

    fun reportAdditionalDiagnostics(
            candidate: ResolvedCallAtom,
            resultingDescriptor: CallableDescriptor,
            kotlinDiagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        reportSmartCasts(candidate, resultingDescriptor, kotlinDiagnosticsHolder)
    }

    private fun createSmartCastDiagnostic(
            candidate: ResolvedCallAtom,
            argument: KotlinCallArgument,
            expectedResultType: UnwrappedType
    ): SmartCastDiagnostic? {
        if (argument !is ExpressionKotlinCallArgument) return null
        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(argument.receiver.receiverValue.type, expectedResultType)) {
            return SmartCastDiagnostic(argument, expectedResultType.unwrap(), candidate.atom)
        }
        return null
    }

    private fun reportSmartCastOnReceiver(
            candidate: ResolvedCallAtom,
            receiver: SimpleKotlinCallArgument?,
            parameter: ReceiverParameterDescriptor?
    ): SmartCastDiagnostic? {
        if (receiver == null || parameter == null) return null
        val expectedType = parameter.type.unwrap().let { if (receiver.isSafeCall) it.makeNullableAsSpecified(true) else it }

        val smartCastDiagnostic = createSmartCastDiagnostic(candidate, receiver, expectedType) ?: return null

        // todo may be we have smart cast to Int?
        return smartCastDiagnostic.takeIf {
            candidate.diagnostics.filterIsInstance<UnsafeCallError>().none {
                it.receiver == receiver
            }
            &&
            candidate.diagnostics.filterIsInstance<UnstableSmartCast>().none {
                it.argument == receiver
            }
        }
    }

    private fun reportSmartCasts(
            candidate: ResolvedCallAtom,
            resultingDescriptor: CallableDescriptor,
            kotlinDiagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        kotlinDiagnosticsHolder.addDiagnosticIfNotNull(reportSmartCastOnReceiver(candidate, candidate.extensionReceiverArgument, resultingDescriptor.extensionReceiverParameter))
        kotlinDiagnosticsHolder.addDiagnosticIfNotNull(reportSmartCastOnReceiver(candidate, candidate.dispatchReceiverArgument, resultingDescriptor.dispatchReceiverParameter))

        for (parameter in resultingDescriptor.valueParameters) {
            for (argument in candidate.argumentMappingByOriginal[parameter.original]?.arguments ?: continue) {
                val smartCastDiagnostic = createSmartCastDiagnostic(candidate, argument, argument.getExpectedType(parameter)) ?: continue

                val thereIsUnstableSmartCastError = candidate.diagnostics.filterIsInstance<UnstableSmartCast>().any {
                    it.argument == argument
                }

                if (!thereIsUnstableSmartCastError) {
                    kotlinDiagnosticsHolder.addDiagnostic(smartCastDiagnostic)
                }
            }
        }
    }
}