/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate.compilation

import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.getCallLabelForLambdaArgument
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.debugger.evaluate.DebuggerFieldPropertyDescriptor
import org.jetbrains.kotlin.idea.debugger.evaluate.EvaluationError
import org.jetbrains.kotlin.idea.debugger.evaluate.EvaluationStatus
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinCodeFragmentFactory.Companion.FAKE_JAVA_CONTEXT_FUNCTION_NAME
import org.jetbrains.kotlin.idea.debugger.evaluate.compilation.CodeFragmentParameter.*
import org.jetbrains.kotlin.idea.debugger.safeLocation
import org.jetbrains.kotlin.idea.debugger.safeMethod
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.isDotSelector
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.checkers.COROUTINE_CONTEXT_1_3_FQ_NAME
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.createFunctionType

class CodeFragmentParameterInfo(
    val parameters: List<Smart>,
    val crossingBounds: Set<Dumb>
)

/*
    The purpose of this class is to figure out what parameters the received code fragment captures.
    It handles both directly mentioned names such as local variables or parameters and implicit values (dispatch/extension receivers).
 */
class CodeFragmentParameterAnalyzer(
    private val context: ExecutionContext,
    private val codeFragment: KtCodeFragment,
    private val bindingContext: BindingContext,
    private val evaluationStatus: EvaluationStatus
) {
    private val parameters = LinkedHashMap<DeclarationDescriptor, Smart>()
    private val crossingBounds = mutableSetOf<Dumb>()

    private val onceUsedChecker = OnceUsedChecker(CodeFragmentParameterAnalyzer::class.java)

    private val containingPrimaryConstructor: ConstructorDescriptor? by lazy {
        context.frameProxy.safeLocation()?.safeMethod()?.takeIf { it.isConstructor } ?: return@lazy null
        val constructor = codeFragment.context?.getParentOfType<KtPrimaryConstructor>(false) ?: return@lazy null
        bindingContext[BindingContext.CONSTRUCTOR, constructor]
    }

    fun analyze(): CodeFragmentParameterInfo {
        onceUsedChecker.trigger()

        codeFragment.accept(object : KtTreeVisitor<Unit>() {
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Unit?): Void? {
                val resolvedCall = expression.getResolvedCall(bindingContext) ?: return null
                processResolvedCall(resolvedCall, expression)

                return null
            }

            private fun processResolvedCall(resolvedCall: ResolvedCall<*>, expression: KtSimpleNameExpression) {
                if (resolvedCall is VariableAsFunctionResolvedCall) {
                    processResolvedCall(resolvedCall.functionCall, expression)
                    processResolvedCall(resolvedCall.variableCall, expression)
                    return
                }

                // Capture dispatch receiver for the extension callable
                run {
                    val descriptor = resolvedCall.resultingDescriptor
                    val containingClass = descriptor?.containingDeclaration as? ClassDescriptor
                    val extensionParameter = descriptor?.extensionReceiverParameter
                    if (descriptor != null && descriptor !is DebuggerFieldPropertyDescriptor
                        && extensionParameter != null && containingClass != null
                    ) {
                        if (containingClass.kind != ClassKind.OBJECT) {
                            val parameter = processDispatchReceiver(containingClass)
                            checkBounds(descriptor, expression, parameter)
                        }
                    }
                }

                if (runReadAction { expression.isDotSelector() }) {
                    // The receiver expression is already captured for this reference
                    return
                }

                if (isCodeFragmentDeclaration(resolvedCall.resultingDescriptor)) {
                    // The reference is from the code fragment we analyze, no need to capture
                    return
                }

                var processed = false

                val extensionReceiver = resolvedCall.extensionReceiver
                if (extensionReceiver is ImplicitReceiver) {
                    val descriptor = extensionReceiver.declarationDescriptor
                    val parameter = processReceiver(extensionReceiver)
                    checkBounds(descriptor, expression, parameter)
                    processed = true
                }

                val dispatchReceiver = resolvedCall.dispatchReceiver
                if (dispatchReceiver is ImplicitReceiver) {
                    val descriptor = dispatchReceiver.declarationDescriptor
                    val parameter = processReceiver(dispatchReceiver)
                    if (parameter != null) {
                        checkBounds(descriptor, expression, parameter)
                        processed = true
                    }
                }

                if (!processed && resolvedCall.resultingDescriptor is SyntheticFieldDescriptor) {
                    val descriptor = resolvedCall.resultingDescriptor as SyntheticFieldDescriptor
                    val parameter = processSyntheticFieldVariable(descriptor)
                    if (parameter != null) {
                        checkBounds(descriptor, expression, parameter)
                        processed = true
                    }
                }

                // If a reference has receivers, we can calculate its value using them, no need to capture
                if (!processed) {
                    val descriptor = resolvedCall.resultingDescriptor
                    val parameter = processDescriptor(descriptor, expression)
                    checkBounds(descriptor, expression, parameter)
                }
            }

            private fun processDescriptor(descriptor: DeclarationDescriptor, expression: KtSimpleNameExpression): Smart? {
                return processDebugLabel(descriptor)
                    ?: processCoroutineContextCall(descriptor)
                    ?: processSimpleNameExpression(descriptor, expression)
            }

            override fun visitThisExpression(expression: KtThisExpression, data: Unit?): Void? {
                val instanceReference = runReadAction { expression.instanceReference }
                val target = bindingContext[BindingContext.REFERENCE_TARGET, instanceReference]

                if (isCodeFragmentDeclaration(target)) {
                    // The reference is from the code fragment we analyze, no need to capture
                    return null
                }

                val parameter = when (target) {
                    is ClassDescriptor -> processDispatchReceiver(target)
                    is CallableDescriptor -> {
                        val type = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type
                        type?.let { processExtensionReceiver(target, type, expression.getLabelName()) }
                    }
                    else -> null
                }

                if (parameter != null) {
                    checkBounds(target, expression, parameter)
                }

                return null
            }

            override fun visitSuperExpression(expression: KtSuperExpression, data: Unit?): Void? {
                val type = bindingContext[BindingContext.THIS_TYPE_FOR_SUPER_EXPRESSION, expression] ?: return null
                val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null

                parameters.getOrPut(descriptor) {
                    val name = descriptor.name.asString()
                    Smart(Dumb(Kind.DISPATCH_RECEIVER, "", "super@$name"), type, descriptor)
                }

                return null
            }

            override fun visitCallExpression(expression: KtCallExpression, data: Unit?): Void? {
                val resolvedCall = expression.getResolvedCall(bindingContext)
                if (resolvedCall != null) {
                    val descriptor = resolvedCall.resultingDescriptor
                    if (descriptor is FunctionDescriptor && descriptor.isSuspend) {
                        evaluationStatus.error(EvaluationError.SuspendCall)
                        throw EvaluateExceptionUtil.createEvaluateException("Evaluation of 'suspend' calls is not supported")
                    }
                    if (descriptor is ConstructorDescriptor && KotlinBuiltIns.isNothing(descriptor.returnType)) {
                        throw EvaluateExceptionUtil.createEvaluateException("'Nothing' can't be instantiated")
                    }
                }

                return super.visitCallExpression(expression, data)
            }
        }, Unit)

        return CodeFragmentParameterInfo(parameters.values.toList(), crossingBounds)
    }

    private fun processReceiver(receiver: ImplicitReceiver): Smart? {
        return when (receiver) {
            is ImplicitClassReceiver -> processDispatchReceiver(receiver.classDescriptor)
            is ExtensionReceiver -> processExtensionReceiver(receiver.declarationDescriptor, receiver.type, null)
            else -> null
        }
    }

    private fun processDispatchReceiver(descriptor: ClassDescriptor): Smart? {
        if (descriptor.kind == ClassKind.OBJECT || containingPrimaryConstructor != null) {
            return null
        }

        val type = descriptor.defaultType
        return parameters.getOrPut(descriptor) {
            val name = descriptor.name
            val debugLabel = if (name.isSpecial) "" else "@" + name.asString()
            Smart(Dumb(Kind.DISPATCH_RECEIVER, "", AsmUtil.THIS + debugLabel), type, descriptor)
        }
    }

    private fun processExtensionReceiver(descriptor: CallableDescriptor, receiverType: KotlinType, label: String?): Smart? {
        if (isFakeFunctionForJavaContext(descriptor)) {
            return processFakeJavaCodeReceiver(descriptor)
        }

        val actualLabel = label ?: getLabel(descriptor) ?: return null
        val receiverParameter = descriptor.extensionReceiverParameter ?: return null

        return parameters.getOrPut(descriptor) {
            Smart(Dumb(Kind.EXTENSION_RECEIVER, actualLabel, AsmUtil.THIS + "@" + actualLabel), receiverType, receiverParameter)
        }
    }

    private fun getLabel(callableDescriptor: CallableDescriptor): String? {
        val source = callableDescriptor.source.getPsi()

        if (source is KtFunctionLiteral) {
            getCallLabelForLambdaArgument(source, bindingContext)?.let { return it }
        }

        return callableDescriptor.name.takeIf { !it.isSpecial }?.asString()
    }

    private fun isFakeFunctionForJavaContext(descriptor: CallableDescriptor): Boolean {
        return descriptor is FunctionDescriptor
                && descriptor.name.asString() == FAKE_JAVA_CONTEXT_FUNCTION_NAME
                && codeFragment.getCopyableUserData(KtCodeFragment.FAKE_CONTEXT_FOR_JAVA_FILE) != null
    }

    private fun processFakeJavaCodeReceiver(descriptor: CallableDescriptor): Smart? {
        val receiverParameter = descriptor
            .takeIf { descriptor is FunctionDescriptor }
            ?.extensionReceiverParameter
            ?: return null

        val label = FAKE_JAVA_CONTEXT_FUNCTION_NAME
        val type = receiverParameter.type
        return parameters.getOrPut(descriptor) {
            Smart(Dumb(Kind.FAKE_JAVA_OUTER_CLASS, label, AsmUtil.THIS), type, receiverParameter)
        }
    }

    private fun processSyntheticFieldVariable(descriptor: SyntheticFieldDescriptor): Smart? {
        val propertyDescriptor = descriptor.propertyDescriptor
        val fieldName = propertyDescriptor.name.asString()
        val type = propertyDescriptor.type
        return parameters.getOrPut(descriptor) {
            Smart(Dumb(Kind.FIELD_VAR, fieldName, "field"), type, descriptor)
        }
    }

    private fun processSimpleNameExpression(target: DeclarationDescriptor, expression: KtSimpleNameExpression): Smart? {
        if (target is ValueParameterDescriptor && target.isCrossinline) {
            evaluationStatus.error(EvaluationError.CrossInlineLambda)
            throw EvaluateExceptionUtil.createEvaluateException("Evaluation of 'crossinline' lambdas is not supported")
        }

        val isLocalTarget = (target as? DeclarationDescriptorWithVisibility)?.visibility == Visibilities.LOCAL

        val isPrimaryConstructorParameter = !isLocalTarget
                && target is PropertyDescriptor
                && isContainingPrimaryConstructorParameter(target)

        if (!isLocalTarget && !isPrimaryConstructorParameter) {
            return null
        }

        return when (target) {
            is SimpleFunctionDescriptor -> {
                val type = target.createFunctionType(target.builtIns, target.isSuspend) ?: return null
                parameters.getOrPut(target) {
                    Smart(Dumb(Kind.LOCAL_FUNCTION, target.name.asString()), type, target.original)
                }
            }
            is ValueDescriptor -> {
                val unwrappedExpression = KtPsiUtil.deparenthesize(expression)
                val isLValue = unwrappedExpression?.let { isAssignmentLValue(it) } ?: false

                parameters.getOrPut(target) {
                    val type = target.type

                    @Suppress("DEPRECATION")
                    val kind = if (target is LocalVariableDescriptor && target.isDelegated) Kind.DELEGATED else Kind.ORDINARY
                    Smart(Dumb(kind, target.name.asString()), type, target, isLValue)
                }
            }
            else -> null
        }
    }

    private fun isAssignmentLValue(expression: PsiElement): Boolean {
        val assignmentExpression = (expression.parent as? KtBinaryExpression)?.takeIf { KtPsiUtil.isAssignment(it) } ?: return false
        return assignmentExpression.left == expression
    }

    private fun isContainingPrimaryConstructorParameter(target: PropertyDescriptor): Boolean {
        val primaryConstructor = containingPrimaryConstructor ?: return false
        for (parameter in primaryConstructor.valueParameters) {
            val property = bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter]
            if (target == property) {
                return true
            }
        }

        return false
    }

    private fun processCoroutineContextCall(target: DeclarationDescriptor): Smart? {
        if (target is PropertyDescriptor && target.fqNameSafe == COROUTINE_CONTEXT_1_3_FQ_NAME) {
            return parameters.getOrPut(target) {
                Smart(Dumb(Kind.COROUTINE_CONTEXT, ""), target.type, target)
            }
        }

        return null
    }

    private fun processDebugLabel(target: DeclarationDescriptor): Smart? {
        val debugLabelPropertyDescriptor = target as? DebugLabelPropertyDescriptor ?: return null
        val labelName = debugLabelPropertyDescriptor.labelName
        val debugString = debugLabelPropertyDescriptor.name.asString()

        return parameters.getOrPut(target) {
            val type = debugLabelPropertyDescriptor.type
            Smart(Dumb(Kind.DEBUG_LABEL, labelName, debugString), type, debugLabelPropertyDescriptor)
        }
    }

    fun checkBounds(descriptor: DeclarationDescriptor?, expression: KtExpression, parameter: Smart?) {
        if (parameter == null || descriptor !is DeclarationDescriptorWithSource) {
            return
        }

        val targetPsi = descriptor.source.getPsi()
        if (targetPsi != null && doesCrossInlineBounds(expression, targetPsi)) {
            crossingBounds += parameter.dumb
        }
    }

    private fun doesCrossInlineBounds(expression: PsiElement, declaration: PsiElement): Boolean {
        val declarationParent = declaration.parent ?: return false
        var currentParent: PsiElement? = expression.parent?.takeIf { it.isInside(declarationParent) } ?: return false

        while (currentParent != null && currentParent != declarationParent) {
            if (currentParent is KtFunction) {
                val functionDescriptor = bindingContext[BindingContext.FUNCTION, currentParent]
                if (functionDescriptor != null && !functionDescriptor.isInline) {
                    return true
                }
            }

            currentParent = when (currentParent) {
                is KtCodeFragment -> currentParent.context
                else -> currentParent.parent
            }
        }

        return false
    }

    private fun isCodeFragmentDeclaration(descriptor: DeclarationDescriptor?): Boolean {
        if (descriptor is ValueParameterDescriptor && isCodeFragmentDeclaration(descriptor.containingDeclaration)) {
            return true
        }

        if (descriptor !is DeclarationDescriptorWithSource) {
            return false
        }

        return descriptor.source.getPsi()?.containingFile is KtCodeFragment
    }

    private tailrec fun PsiElement.isInside(parent: PsiElement): Boolean {
        if (parent.isAncestor(this)) {
            return true
        }

        val context = (this.containingFile as? KtCodeFragment)?.context ?: return false
        return context.isInside(parent)
    }
}

private class OnceUsedChecker(private val clazz: Class<*>) {
    private var used = false

    fun trigger() {
        if (used) {
            error(clazz.name + " may be only used once")
        }

        used = true
    }
}