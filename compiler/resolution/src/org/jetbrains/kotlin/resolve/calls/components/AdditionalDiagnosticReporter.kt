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

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.expandIntersectionTypeIfNecessary

// very initial state of component
// todo: handle all diagnostic inside DiagnosticReporterByTrackingStrategy
// move it to frontend module
class AdditionalDiagnosticReporter(private val languageVersionSettings: LanguageVersionSettings) {

    fun reportAdditionalDiagnostics(
        candidate: ResolvedCallAtom,
        resultingDescriptor: CallableDescriptor,
        kotlinDiagnosticsHolder: KotlinDiagnosticsHolder,
        diagnostics: Collection<KotlinCallDiagnostic>
    ) {
        reportSmartCasts(candidate, resultingDescriptor, kotlinDiagnosticsHolder, diagnostics)
    }

    private fun createSmartCastDiagnostic(
        candidate: ResolvedCallAtom,
        argument: KotlinCallArgument,
        expectedResultType: UnwrappedType
    ): SmartCastDiagnostic? {
        if (argument !is ExpressionKotlinCallArgument) return null

        val types = expectedResultType.expandIntersectionTypeIfNecessary()

        val argumentType = argument.receiver.receiverValue.type
        val isSubtype = types.map { KotlinTypeChecker.DEFAULT.isSubtypeOf(argumentType, it) }
        if (isSubtype.any { it }) return null

        return SmartCastDiagnostic(argument, types.first().unwrap(), candidate.atom)
    }

    private fun reportSmartCastOnReceiver(
        candidate: ResolvedCallAtom,
        receiver: SimpleKotlinCallArgument?,
        parameter: ReceiverParameterDescriptor?,
        diagnostics: Collection<KotlinCallDiagnostic>
    ): SmartCastDiagnostic? {
        if (receiver == null || parameter == null) return null
        val expectedType = parameter.type.unwrap().let { if (receiver.isSafeCall) it.makeNullableAsSpecified(true) else it }

        val smartCastDiagnostic = createSmartCastDiagnostic(candidate, receiver, expectedType) ?: return null

        // todo may be we have smart cast to Int?
        return smartCastDiagnostic.takeIf {
            diagnostics.filterIsInstance<UnsafeCallError>().none {
                it.receiver == receiver
            }
                    &&
                    diagnostics.filterIsInstance<UnstableSmartCast>().none {
                        it.argument == receiver
                    }
        }
    }

    private fun reportSmartCasts(
        candidate: ResolvedCallAtom,
        resultingDescriptor: CallableDescriptor,
        kotlinDiagnosticsHolder: KotlinDiagnosticsHolder,
        diagnostics: Collection<KotlinCallDiagnostic>
    ) {
        kotlinDiagnosticsHolder.addDiagnosticIfNotNull(
            reportSmartCastOnReceiver(
                candidate,
                candidate.extensionReceiverArgument,
                resultingDescriptor.extensionReceiverParameter,
                diagnostics
            )
        )
        kotlinDiagnosticsHolder.addDiagnosticIfNotNull(
            reportSmartCastOnReceiver(
                candidate,
                candidate.dispatchReceiverArgument,
                resultingDescriptor.dispatchReceiverParameter,
                diagnostics
            )
        )

        for (parameter in resultingDescriptor.valueParameters) {
            for (argument in candidate.argumentMappingByOriginal[parameter.original]?.arguments ?: continue) {
                val effectiveExpectedType = argument.getExpectedType(parameter, languageVersionSettings)
                val smartCastDiagnostic = createSmartCastDiagnostic(candidate, argument, effectiveExpectedType) ?: continue

                val thereIsUnstableSmartCastError = diagnostics.filterIsInstance<UnstableSmartCast>().any {
                    it.argument == argument
                }

                if (!thereIsUnstableSmartCastError) {
                    kotlinDiagnosticsHolder.addDiagnostic(smartCastDiagnostic)
                }
            }
        }
    }
}