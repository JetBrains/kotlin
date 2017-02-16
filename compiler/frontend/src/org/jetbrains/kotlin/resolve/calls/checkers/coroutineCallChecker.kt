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
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.coroutines.hasSuspendFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.hasRestrictsSuspensionAnnotation
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object CoroutineSuspendCallChecker : CallChecker {
    private val ALLOWED_SCOPE_KINDS = setOf(LexicalScopeKind.FUNCTION_INNER_SCOPE, LexicalScopeKind.FUNCTION_HEADER_FOR_DESTRUCTURING)

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor as? FunctionDescriptor ?: return
        if (!descriptor.isSuspend) return

        val enclosingSuspendFunction =
                context.scope
                        .parentsWithSelf.firstOrNull {
                                it is LexicalScope && it.kind in ALLOWED_SCOPE_KINDS &&
                                    it.ownerDescriptor.safeAs<FunctionDescriptor>()?.isSuspend == true
                        }?.cast<LexicalScope>()?.ownerDescriptor?.cast<FunctionDescriptor>()

        when {
            enclosingSuspendFunction != null -> {
                val callElement = resolvedCall.call.callElement as KtExpression

                if (!InlineUtil.checkNonLocalReturnUsage(enclosingSuspendFunction, callElement, context.resolutionContext)) {
                    context.trace.report(Errors.NON_LOCAL_SUSPENSION_POINT.on(reportOn))
                }
                else if (context.scope.parentsWithSelf.any { it.isScopeForDefaultParameterValuesOf(enclosingSuspendFunction) }) {
                    context.trace.report(Errors.UNSUPPORTED.on(reportOn, "suspend function calls in a context of default parameter value"))
                }

                context.trace.record(BindingContext.ENCLOSING_SUSPEND_FUNCTION_FOR_SUSPEND_FUNCTION_CALL, resolvedCall.call, enclosingSuspendFunction)

                checkRestrictsSuspension(enclosingSuspendFunction, resolvedCall, reportOn, context)
            }
            else -> {
                context.trace.report(Errors.ILLEGAL_SUSPEND_FUNCTION_CALL.on(reportOn, resolvedCall.candidateDescriptor))
            }
        }
    }
}

private fun HierarchicalScope.isScopeForDefaultParameterValuesOf(enclosingSuspendFunction: FunctionDescriptor) =
        this is LexicalScope && this.kind == LexicalScopeKind.DEFAULT_VALUE && this.ownerDescriptor == enclosingSuspendFunction

object BuilderFunctionsCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor as? FunctionDescriptor ?: return
        if (descriptor.valueParameters.any { it.hasSuspendFunctionType }) {
            checkCoroutinesFeature(context.languageVersionSettings, context.trace, reportOn)
        }
    }
}

fun checkCoroutinesFeature(languageVersionSettings: LanguageVersionSettings, diagnosticHolder: DiagnosticSink, reportOn: PsiElement) {
    val diagnosticData = LanguageFeature.Coroutines to languageVersionSettings
    if (!languageVersionSettings.supportsFeature(LanguageFeature.Coroutines)) {
        diagnosticHolder.report(Errors.UNSUPPORTED_FEATURE.on(reportOn, diagnosticData))
    }
    else if (languageVersionSettings.supportsFeature(LanguageFeature.ErrorOnCoroutines)) {
        diagnosticHolder.report(Errors.EXPERIMENTAL_FEATURE_ERROR.on(reportOn, diagnosticData))
    }
    else if (!languageVersionSettings.supportsFeature(LanguageFeature.DoNotWarnOnCoroutines)) {
        diagnosticHolder.report(Errors.EXPERIMENTAL_FEATURE_WARNING.on(reportOn, diagnosticData))
    }
}

private fun checkRestrictsSuspension(
        enclosingCallableDescriptor: CallableDescriptor,
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
) {
    val enclosingSuspendReceiverValue = enclosingCallableDescriptor.extensionReceiverParameter?.value ?: return

    fun ReceiverValue.isRestrictsSuspensionReceiver() = (type.supertypes() + type).any {
        it.constructor.declarationDescriptor?.hasRestrictsSuspensionAnnotation() == true
    }

    infix fun ReceiverValue.sameInstance(other: ReceiverValue?): Boolean {
        if (other == null) return false
        if (this === other) return true

        val referenceExpression = ((other as? ExpressionReceiver)?.expression as? KtThisExpression)?.instanceReference
        val referenceTarget = referenceExpression?.let {
            context.trace.get(BindingContext.REFERENCE_TARGET, referenceExpression)
        }

        return this === (referenceTarget as? CallableDescriptor)?.extensionReceiverParameter?.value
    }

    if (!enclosingSuspendReceiverValue.isRestrictsSuspensionReceiver()) return

    // member of suspend receiver
    if (enclosingSuspendReceiverValue sameInstance resolvedCall.dispatchReceiver) return

    if (enclosingSuspendReceiverValue sameInstance resolvedCall.extensionReceiver &&
        resolvedCall.candidateDescriptor.extensionReceiverParameter!!.value.isRestrictsSuspensionReceiver()) return

    context.trace.report(Errors.ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL.on(reportOn))
}
