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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.results.isSignatureNotLessSpecific
import org.jetbrains.kotlin.resolve.descriptorUtil.hasHidesMembersAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.varargParameterPosition
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.util.OperatorNameConventions

class ShadowedExtensionChecker(val typeSpecificityComparator: TypeSpecificityComparator, val trace: DiagnosticSink) {
    fun checkDeclaration(declaration: KtDeclaration, descriptor: DeclarationDescriptor) {
        if (declaration.name == null) return
        if (descriptor !is CallableMemberDescriptor) return
        if (descriptor.hasHidesMembersAnnotation()) return
        val extensionReceiverType = descriptor.extensionReceiverParameter?.type ?: return
        if (extensionReceiverType.isError) return
        if (extensionReceiverType.isMarkedNullable) return

        when (descriptor) {
            is FunctionDescriptor ->
                checkShadowedExtensionFunction(declaration, descriptor, trace)
            is PropertyDescriptor ->
                checkShadowedExtensionProperty(declaration, descriptor, trace)
        }
    }

    private fun checkShadowedExtensionFunction(declaration: KtDeclaration, extensionFunction: FunctionDescriptor, trace: DiagnosticSink) {
        val memberScope = extensionFunction.extensionReceiverParameter?.type?.memberScope ?: return

        for (memberFunction in memberScope.getContributedFunctions(extensionFunction.name, NoLookupLocation.WHEN_CHECK_DECLARATION_CONFLICTS)) {
            if (memberFunction.isPublic() && isExtensionFunctionShadowedByMemberFunction(extensionFunction, memberFunction)) {
                trace.report(Errors.EXTENSION_SHADOWED_BY_MEMBER.on(declaration, memberFunction))
                return
            }
        }

        val nestedClass = memberScope.getContributedClassifier(extensionFunction.name, NoLookupLocation.WHEN_CHECK_DECLARATION_CONFLICTS)
        if (nestedClass is ClassDescriptor && nestedClass.isInner && nestedClass.isPublic()) {
            for (constructor in nestedClass.constructors) {
                if (constructor.isPublic() && isExtensionFunctionShadowedByMemberFunction(extensionFunction, constructor)) {
                    trace.report(Errors.EXTENSION_FUNCTION_SHADOWED_BY_INNER_CLASS_CONSTRUCTOR.on(declaration, constructor))
                    return
                }
            }
        }

        for (memberProperty in memberScope.getContributedVariables(extensionFunction.name, NoLookupLocation.WHEN_CHECK_DECLARATION_CONFLICTS)) {
            if (!memberProperty.isPublic()) continue

            val invokeOperator = getInvokeOperatorShadowingExtensionFunction(extensionFunction, memberProperty)
            if (invokeOperator != null) {
                trace.report(Errors.EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE.on(declaration, memberProperty, invokeOperator))
                return
            }
        }
    }

    private fun DeclarationDescriptorWithVisibility.isPublic() =
            visibility.normalize() == Visibilities.PUBLIC

    private fun isExtensionFunctionShadowedByMemberFunction(extension: FunctionDescriptor, member: FunctionDescriptor): Boolean {
        // Permissive check:
        //      (1) functions should have same number of arguments;
        //      (2) varargs should be in the same positions;
        //      (3) extension signature should be not less specific than member signature.
        // (1) & (2) are required so that we can match signatures easily.

        if (extension.valueParameters.size != member.valueParameters.size) return false
        if (extension.varargParameterPosition() != member.varargParameterPosition()) return false
        if (extension.isOperator && !member.isOperator) return false
        if (extension.isInfix && !member.isInfix) return false

        val extensionSignature = FlatSignature.createForPossiblyShadowedExtension(extension)
        val memberSignature = FlatSignature.createFromCallableDescriptor(member)
        return isSignatureNotLessSpecific(extensionSignature, memberSignature)
    }

    private fun getInvokeOperatorShadowingExtensionFunction(extension: FunctionDescriptor, member: PropertyDescriptor): FunctionDescriptor? =
            member.type.memberScope.getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.WHEN_CHECK_DECLARATION_CONFLICTS)
                    .firstOrNull { it.isPublic() && it.isOperator && isExtensionFunctionShadowedByMemberFunction(extension, it) }

    private fun isSignatureNotLessSpecific(extensionSignature: FlatSignature<FunctionDescriptor>, memberSignature: FlatSignature<FunctionDescriptor>): Boolean =
            ConstraintSystemBuilderImpl.forSpecificity().isSignatureNotLessSpecific(
                    extensionSignature,
                    memberSignature,
                    OverloadabilitySpecificityCallbacks,
                    typeSpecificityComparator
            )

    private fun checkShadowedExtensionProperty(declaration: KtDeclaration, extensionProperty: PropertyDescriptor, trace: DiagnosticSink) {
        val memberScope = extensionProperty.extensionReceiverParameter?.type?.memberScope ?: return

        memberScope.getContributedVariables(extensionProperty.name, NoLookupLocation.WHEN_CHECK_DECLARATION_CONFLICTS)
                .firstOrNull { it.isPublic() && !it.isExtension }
                ?.let { memberProperty ->
                    trace.report(Errors.EXTENSION_SHADOWED_BY_MEMBER.on(declaration, memberProperty))
                }
    }

}