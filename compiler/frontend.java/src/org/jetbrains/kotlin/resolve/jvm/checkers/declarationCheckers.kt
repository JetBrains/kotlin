/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.checkers.SimpleDeclarationChecker
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class LocalFunInlineChecker : SimpleDeclarationChecker {

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext) {
        if (InlineUtil.isInline(descriptor) &&
            declaration is KtNamedFunction &&
            descriptor is FunctionDescriptor &&
            descriptor.visibility == Visibilities.LOCAL) {
            diagnosticHolder.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(declaration, declaration, descriptor))
        }
    }
}

class PlatformStaticAnnotationChecker : SimpleDeclarationChecker {

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor.hasJvmStaticAnnotation()) {
            if (declaration is KtNamedFunction ||
                declaration is KtProperty ||
                declaration is KtPropertyAccessor ||
                declaration is KtParameter) {
                checkDeclaration(declaration, descriptor, diagnosticHolder)
            }
        }
    }

    private fun checkDeclaration(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink
    ) {
        val container = descriptor.containingDeclaration
        val insideObject = container != null && DescriptorUtils.isNonCompanionObject(container)
        val insideCompanionObjectInClass =
                container != null && DescriptorUtils.isCompanionObject(container) &&
                DescriptorUtils.isClassOrEnumClass(container.containingDeclaration)

        if (!insideObject && !insideCompanionObjectInClass) {
            diagnosticHolder.report(ErrorsJvm.JVM_STATIC_NOT_IN_OBJECT.on(declaration))
        }

        val checkDeclaration = when(declaration) {
            is KtPropertyAccessor -> declaration.getParent() as KtProperty
            else -> declaration
        }

        if (insideObject && checkDeclaration.getModifierList()?.hasModifier(KtTokens.OVERRIDE_KEYWORD) == true) {
            diagnosticHolder.report(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC.on(declaration))
        }

        if (descriptor is PropertyDescriptor && (descriptor.isConst || descriptor.hasJvmFieldAnnotation())) {
            diagnosticHolder.report(ErrorsJvm.JVM_STATIC_ON_CONST_OR_JVM_FIELD.on(declaration))
        }
    }
}

class JvmNameAnnotationChecker : SimpleDeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        val platformNameAnnotation = DescriptorUtils.getJvmNameAnnotation(descriptor)
        if (platformNameAnnotation != null) {
            checkDeclaration(descriptor, platformNameAnnotation, diagnosticHolder)
        }
    }

    private fun checkDeclaration(descriptor: DeclarationDescriptor,
                                 annotation: AnnotationDescriptor,
                                 diagnosticHolder: DiagnosticSink) {
        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return

        if (descriptor is FunctionDescriptor && !isRenamableFunction(descriptor)) {
            diagnosticHolder.report(ErrorsJvm.INAPPLICABLE_JVM_NAME.on(annotationEntry))
        }

        val value = DescriptorUtils.getJvmName(descriptor)
        if (value == null || !Name.isValidIdentifier(value)) {
            diagnosticHolder.report(ErrorsJvm.ILLEGAL_JVM_NAME.on(annotationEntry))
        }

        if (descriptor is CallableMemberDescriptor) {
            val callableMemberDescriptor = descriptor
            if (DescriptorUtils.isOverride(callableMemberDescriptor) || callableMemberDescriptor.isOverridable) {
                diagnosticHolder.report(ErrorsJvm.INAPPLICABLE_JVM_NAME.on(annotationEntry))
            }
        }
    }

    private fun isRenamableFunction(descriptor: FunctionDescriptor): Boolean {
        val containingDescriptor = descriptor.containingDeclaration

        return containingDescriptor is PackageFragmentDescriptor || containingDescriptor is ClassDescriptor
    }
}

class VolatileAnnotationChecker : SimpleDeclarationChecker {

    override fun check(declaration: KtDeclaration,
                       descriptor: DeclarationDescriptor,
                       diagnosticHolder: DiagnosticSink,
                       bindingContext: BindingContext
    ) {
        val volatileAnnotation = DescriptorUtils.getVolatileAnnotation(descriptor)
        if (volatileAnnotation != null) {
            if (descriptor is PropertyDescriptor && !descriptor.isVar) {
                val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(volatileAnnotation) ?: return
                diagnosticHolder.report(ErrorsJvm.VOLATILE_ON_VALUE.on(annotationEntry))
            }
            if (declaration is KtProperty && declaration.hasDelegate()) {
                val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(volatileAnnotation) ?: return
                diagnosticHolder.report(ErrorsJvm.VOLATILE_ON_DELEGATE.on(annotationEntry))
            }
        }
    }
}

class SynchronizedAnnotationChecker : SimpleDeclarationChecker {

    override fun check(declaration: KtDeclaration,
                       descriptor: DeclarationDescriptor,
                       diagnosticHolder: DiagnosticSink,
                       bindingContext: BindingContext
    ) {
        val synchronizedAnnotation = DescriptorUtils.getSynchronizedAnnotation(descriptor)
        if (synchronizedAnnotation != null && descriptor is FunctionDescriptor && descriptor.modality == Modality.ABSTRACT) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(synchronizedAnnotation) ?: return
            diagnosticHolder.report(ErrorsJvm.SYNCHRONIZED_ON_ABSTRACT.on(annotationEntry))
        }
    }
}

class OverloadsAnnotationChecker: SimpleDeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        descriptor.findJvmOverloadsAnnotation()?.let { annotation ->
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation)
            if (annotationEntry != null) {
                checkDeclaration(annotationEntry, descriptor, diagnosticHolder)
            }
        }
    }

    private fun checkDeclaration(annotationEntry: KtAnnotationEntry, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor !is CallableDescriptor) {
            return
        }
        if ((descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.INTERFACE) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_INTERFACE.on(annotationEntry))
        }
        else if (descriptor is FunctionDescriptor && descriptor.modality == Modality.ABSTRACT) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_ABSTRACT.on(annotationEntry))
        }
        else if (DescriptorUtils.isLocal(descriptor)) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_LOCAL.on(annotationEntry))
        }
        else if (!descriptor.visibility.isPublicAPI && descriptor.visibility != Visibilities.INTERNAL) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_PRIVATE.on(annotationEntry))
        }
        else if (descriptor.valueParameters.none { it.declaresDefaultValue() }) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS.on(annotationEntry))
        }
    }
}

class TypeParameterBoundIsNotArrayChecker : SimpleDeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        val typeParameters = (descriptor as? CallableDescriptor)?.typeParameters
                             ?: (descriptor as? ClassDescriptor)?.declaredTypeParameters
                             ?: return

        for (typeParameter in typeParameters) {
            if (typeParameter.upperBounds.any { KotlinBuiltIns.isArray(it) || KotlinBuiltIns.isPrimitiveArray(it) }) {
                val element = DescriptorToSourceUtils.descriptorToDeclaration(typeParameter) ?: declaration
                diagnosticHolder.report(ErrorsJvm.UPPER_BOUND_CANNOT_BE_ARRAY.on(element))
            }
        }
    }
}