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

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getSuperCallExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.*

class InterfaceDefaultMethodCallChecker(val jvmTarget: JvmTarget) : CallChecker {

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val supportDefaults = jvmTarget >= JvmTarget.JVM_1_8

        val descriptor = resolvedCall.resultingDescriptor as? CallableMemberDescriptor ?: return
        if (descriptor is JavaPropertyDescriptor) return

        if (!supportDefaults &&
            isStaticDeclaration(descriptor) &&
            isInterface(descriptor.containingDeclaration) &&
            descriptor is JavaCallableMemberDescriptor) {
            val diagnostic = if (isDefaultCallsProhibited(context)) INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR else INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET
            context.trace.report(diagnostic.on(reportOn))
        }

        val superCallExpression = getSuperCallExpression(resolvedCall.call) ?: return

        if (!isInterface(descriptor.original.containingDeclaration)) return

        val realDescriptor = unwrapFakeOverride(descriptor)
        val realDescriptorOwner = realDescriptor.containingDeclaration as? ClassDescriptor ?: return

        if (isInterface(realDescriptorOwner) && (realDescriptor is JavaCallableMemberDescriptor || realDescriptor.hasJvmDefaultAnnotation())) {
            val bindingContext = context.trace.bindingContext
            val thisForSuperCall = getSuperCallLabelTarget(bindingContext, superCallExpression)

            if (thisForSuperCall != null && DescriptorUtils.isInterface(thisForSuperCall)) {
                val declarationWithCall = findInterfaceMember(thisForSuperCall, superCallExpression, bindingContext)
                if (declarationWithCall?.hasJvmDefaultAnnotation() == false) {
                    context.trace.report(INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER.on(reportOn))
                    return
                }
            }

            if (!supportDefaults) {
                val diagnostic =
                    if (isDefaultCallsProhibited(context)) DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET_ERROR else DEFAULT_METHOD_CALL_FROM_JAVA6_TARGET
                context.trace.report(diagnostic.on(reportOn))
            }
        }
    }

    private fun findInterfaceMember(
        descriptorToSearch: ClassDescriptor,
        startExpression: KtSuperExpression,
        bindingContext: BindingContext
    ): CallableMemberDescriptor? {
        val parents = generateSequence({ startExpression.parent }) { it.parent }
        parents.fold<PsiElement, PsiElement>(startExpression) { child, parent ->
            if (parent is KtClassBody &&
                descriptorToSearch == bindingContext.get(BindingContext.CLASS, parent.parent)
            ) {
                return when (child) {
                    is KtNamedFunction -> bindingContext.get(BindingContext.FUNCTION, child)
                    is KtProperty -> bindingContext.get(BindingContext.VARIABLE, child) as? PropertyDescriptor
                    else -> null
                }
            } else parent
        }

        return null
    }

    private fun isDefaultCallsProhibited(context: CallCheckerContext) =
        context.languageVersionSettings.supportsFeature(LanguageFeature.DefaultMethodsCallFromJava6TargetError)

    private fun getSuperCallLabelTarget(
        bindingContext: BindingContext,
        expression: KtSuperExpression
    ): ClassDescriptor? {
        val thisTypeForSuperCall = bindingContext.get(BindingContext.THIS_TYPE_FOR_SUPER_EXPRESSION, expression) ?: return null
        val descriptor = thisTypeForSuperCall.constructor.declarationDescriptor
        return descriptor as? ClassDescriptor
    }

}
