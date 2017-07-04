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

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceivers
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType

object DslScopeViolationCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.DslMarkersSupport)) return
        val callImplicitReceivers = resolvedCall.getImplicitReceivers()

        for (callImplicitReceiver in callImplicitReceivers) {
            checkCallImplicitReceiver(callImplicitReceiver, resolvedCall, reportOn, context)
        }
    }

    private fun checkCallImplicitReceiver(
            callImplicitReceiver: ReceiverValue,
            resolvedCall: ResolvedCall<*>,
            reportOn: PsiElement,
            context: CallCheckerContext
    ) {
        val receiversUntilOneFromTheCall =
                context.scope.parentsWithSelf
                        .mapNotNull { (it as? LexicalScope)?.implicitReceiver?.value }
                        .takeWhile { it != callImplicitReceiver }.toList()

        if (receiversUntilOneFromTheCall.isEmpty()) return

        val callDslMarkers = callImplicitReceiver.type.extractDslMarkerFqNames()
        if (callDslMarkers.isEmpty()) return

        val closestAnotherReceiverWithSameDslMarker =
                receiversUntilOneFromTheCall.firstOrNull { receiver -> receiver.type.extractDslMarkerFqNames().any(callDslMarkers::contains) }

        if (closestAnotherReceiverWithSameDslMarker != null) {
            // TODO: report receivers configuration (what's one is used and what's one is the closest)
            context.trace.report(Errors.DSL_SCOPE_VIOLATION.on(reportOn, resolvedCall.resultingDescriptor))
        }
    }

    fun KotlinType.extractDslMarkerFqNames(): Set<FqName> {
        val result = mutableSetOf<FqName>()

        result.addAll(annotations.extractDslMarkerFqNames())

        constructor.declarationDescriptor?.getAllSuperClassifiers()?.asIterable()
                ?.flatMapTo(result) { it.annotations.extractDslMarkerFqNames() }

        return result
    }

}

private fun Annotations.extractDslMarkerFqNames() =
        filter(AnnotationDescriptor::isDslMarker).map { it.fqName!! }

private fun AnnotationDescriptor.isDslMarker(): Boolean {
    val classDescriptor = annotationClass ?: return false
    return classDescriptor.annotations.hasAnnotation(DSL_MARKER_FQ_NAME)
}

private val DSL_MARKER_FQ_NAME = FqName("kotlin.DslMarker")
