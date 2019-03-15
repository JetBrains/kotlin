/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.coroutines.hasSuspendFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val COROUTINE_CONTEXT_1_2_20_FQ_NAME =
    DescriptorUtils.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME_EXPERIMENTAL.child(Name.identifier("coroutineContext"))

val COROUTINE_CONTEXT_1_2_30_FQ_NAME =
    DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_EXPERIMENTAL.child(Name.identifier("coroutineContext"))

val COROUTINE_CONTEXT_1_3_FQ_NAME =
    DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_RELEASE.child(Name.identifier("coroutineContext"))

fun FunctionDescriptor.isBuiltInCoroutineContext(languageVersionSettings: LanguageVersionSettings) =
    (this as? PropertyGetterDescriptor)?.correspondingProperty?.fqNameSafe?.isBuiltInCoroutineContext(languageVersionSettings) == true

fun PropertyDescriptor.isBuiltInCoroutineContext(languageVersionSettings: LanguageVersionSettings) =
    this.fqNameSafe.isBuiltInCoroutineContext(languageVersionSettings)

private val ALLOWED_SCOPE_KINDS = setOf(LexicalScopeKind.FUNCTION_INNER_SCOPE, LexicalScopeKind.FUNCTION_HEADER_FOR_DESTRUCTURING)

fun findEnclosingSuspendFunction(context: CallCheckerContext): FunctionDescriptor? = context.scope
    .parentsWithSelf.firstOrNull {
    it is LexicalScope && it.kind in ALLOWED_SCOPE_KINDS &&
            it.ownerDescriptor.safeAs<FunctionDescriptor>()?.isSuspend == true
}?.cast<LexicalScope>()?.ownerDescriptor?.cast()

object CoroutineSuspendCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor
        when (descriptor) {
            is FunctionDescriptor -> if (!descriptor.isSuspend) return
            is PropertyDescriptor -> when (descriptor.fqNameSafe) {
                COROUTINE_CONTEXT_1_2_20_FQ_NAME, COROUTINE_CONTEXT_1_2_30_FQ_NAME, COROUTINE_CONTEXT_1_3_FQ_NAME -> {
                }
                else -> return
            }
            else -> return
        }

        val enclosingSuspendFunction = findEnclosingSuspendFunction(context)

        when {
            enclosingSuspendFunction != null -> {
                val callElement = resolvedCall.call.callElement as KtExpression

                if (!InlineUtil.checkNonLocalReturnUsage(enclosingSuspendFunction, callElement, context.resolutionContext)) {
                    var shouldReport = true

                    // Do not report for KtCodeFragment in a suspend function context
                    val containingFile = callElement.containingFile
                    if (containingFile is KtCodeFragment) {
                        val c = containingFile.context?.getParentOfType<KtExpression>(false)
                        if (c != null && InlineUtil.checkNonLocalReturnUsage(enclosingSuspendFunction, c, context.resolutionContext)) {
                            shouldReport = false
                        }
                    }

                    if (shouldReport) {
                        context.trace.report(Errors.NON_LOCAL_SUSPENSION_POINT.on(reportOn))
                    }
                } else if (context.scope.parentsWithSelf.any { it.isScopeForDefaultParameterValuesOf(enclosingSuspendFunction) }) {
                    context.trace.report(Errors.UNSUPPORTED.on(reportOn, "suspend function calls in a context of default parameter value"))
                }

                if ((descriptor.fqNameSafe == COROUTINE_CONTEXT_1_2_20_FQ_NAME || descriptor.fqNameSafe == COROUTINE_CONTEXT_1_2_30_FQ_NAME) &&
                    context.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
                ) {
                    context.trace.report(
                        Errors.UNSUPPORTED.on(
                            reportOn,
                            "experimental coroutineContext of release coroutine: use kotlin.coroutines.coroutineContext instead"
                        )
                    )
                }

                context.trace.record(
                    BindingContext.ENCLOSING_SUSPEND_FUNCTION_FOR_SUSPEND_FUNCTION_CALL,
                    resolvedCall.call,
                    enclosingSuspendFunction
                )

                checkRestrictsSuspension(enclosingSuspendFunction, resolvedCall, reportOn, context)
            }
            resolvedCall.call.isCallableReference() -> {
                // do nothing: we can get callable reference to suspend function outside suspend context
            }
            else -> {
                when (descriptor) {
                    is FunctionDescriptor -> context.trace.report(
                        Errors.ILLEGAL_SUSPEND_FUNCTION_CALL.on(
                            reportOn,
                            resolvedCall.candidateDescriptor
                        )
                    )
                    is PropertyDescriptor -> context.trace.report(
                        Errors.ILLEGAL_SUSPEND_PROPERTY_ACCESS.on(
                            reportOn,
                            resolvedCall.candidateDescriptor
                        )
                    )
                }
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
    if (languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)) {
        if (languageVersionSettings.apiVersion < ApiVersion.KOTLIN_1_3) {
            diagnosticHolder.report(Errors.UNSUPPORTED.on(reportOn, "cannot use release coroutines with api version less than 1.3"))
        }
        return
    }
    val diagnosticData = LanguageFeature.Coroutines to languageVersionSettings
    when (languageVersionSettings.getFeatureSupport(LanguageFeature.Coroutines)) {
        LanguageFeature.State.ENABLED -> {
        }
        LanguageFeature.State.ENABLED_WITH_WARNING -> {
            diagnosticHolder.report(Errors.EXPERIMENTAL_FEATURE_WARNING.on(reportOn, diagnosticData))
        }
        LanguageFeature.State.ENABLED_WITH_ERROR -> {
            diagnosticHolder.report(Errors.EXPERIMENTAL_FEATURE_ERROR.on(reportOn, diagnosticData))
        }
        LanguageFeature.State.DISABLED -> {
            diagnosticHolder.report(Errors.UNSUPPORTED_FEATURE.on(reportOn, diagnosticData))
        }
    }
}

fun KotlinType.isRestrictsSuspensionReceiver(languageVersionSettings: LanguageVersionSettings) = (listOf(this) + this.supertypes()).any {
    it.constructor.declarationDescriptor?.annotations?.hasAnnotation(languageVersionSettings.restrictsSuspensionFqName()) == true
}

private fun checkRestrictsSuspension(
    enclosingSuspendCallableDescriptor: CallableDescriptor,
    resolvedCall: ResolvedCall<*>,
    reportOn: PsiElement,
    context: CallCheckerContext
) {
    fun ReceiverValue.isRestrictsSuspensionReceiver() = type.isRestrictsSuspensionReceiver(context.languageVersionSettings)

    infix fun ReceiverValue.sameInstance(other: ReceiverValue?): Boolean {
        if (other == null) return false
        // Implicit receiver should be reference equal
        if (this.original === other.original) return true

        val referenceExpression = ((other as? ExpressionReceiver)?.expression as? KtThisExpression)?.instanceReference
        val referenceTarget = referenceExpression?.let {
            context.trace.get(BindingContext.REFERENCE_TARGET, referenceExpression)
        }

        val referenceReceiverValue = when (referenceTarget) {
            is CallableDescriptor -> referenceTarget.extensionReceiverParameter?.value
            is ClassDescriptor -> referenceTarget.thisAsReceiverParameter.value
            else -> null
        }

        return this === referenceReceiverValue
    }

    fun reportError() {
        context.trace.report(Errors.ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL.on(reportOn))
    }

    val enclosingSuspendExtensionReceiverValue = enclosingSuspendCallableDescriptor.extensionReceiverParameter?.value
    val enclosingSuspendDispatchReceiverValue = enclosingSuspendCallableDescriptor.dispatchReceiverParameter?.value

    val receivers = listOfNotNull(resolvedCall.dispatchReceiver, resolvedCall.extensionReceiver)
    for (receiverValue in receivers) {
        if (!receiverValue.isRestrictsSuspensionReceiver()) continue
        if (enclosingSuspendExtensionReceiverValue?.sameInstance(receiverValue) == true) continue
        if (enclosingSuspendDispatchReceiverValue?.sameInstance(receiverValue) == true) continue

        reportError()
        return
    }

    if (enclosingSuspendExtensionReceiverValue?.isRestrictsSuspensionReceiver() != true) return

    // member of suspend receiver
    if (enclosingSuspendExtensionReceiverValue sameInstance resolvedCall.dispatchReceiver) return

    if (enclosingSuspendExtensionReceiverValue sameInstance resolvedCall.extensionReceiver &&
        resolvedCall.candidateDescriptor.extensionReceiverParameter!!.value.isRestrictsSuspensionReceiver()
    ) return

    reportError()
}
