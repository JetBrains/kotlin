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

import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.psi.KtxElement
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade

class ComposeKtxTypeResolutionExtension : KtxTypeResolutionExtension {

    override fun visitKtxElement(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade,
        callResolver: CallResolver
    ) {
        val ktxCallResolver =
            KtxCallResolver(
                callResolver,
                facade,
                element.project,
                ComposableAnnotationChecker.get(element.project)
            )

        ktxCallResolver.initializeFromKtxElement(element, context)

        val temporaryForKtxCall =
            TemporaryTraceAndCache.create(context, "trace to resolve ktx call", element)

        val resolvedKtxElementCall = ktxCallResolver.resolveFromKtxElement(
            element,
            context.replaceTraceAndCache(temporaryForKtxCall)
        )

        temporaryForKtxCall.commit()

        context.trace.record(ComposeWritableSlices.RESOLVED_KTX_CALL, element, resolvedKtxElementCall)
    }
}