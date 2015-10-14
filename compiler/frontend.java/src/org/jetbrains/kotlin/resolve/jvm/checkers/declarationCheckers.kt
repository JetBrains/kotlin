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
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DeclarationChecker
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.hasInlineAnnotation
import org.jetbrains.kotlin.resolve.annotations.hasIntrinsicAnnotation
import org.jetbrains.kotlin.resolve.annotations.hasPlatformStaticAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmOverloadsAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

public class LocalFunInlineChecker : DeclarationChecker {

    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext) {
        if (descriptor.hasInlineAnnotation() &&
            declaration is JetNamedFunction &&
            descriptor is FunctionDescriptor &&
            descriptor.getVisibility() == Visibilities.LOCAL) {
            diagnosticHolder.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(declaration, declaration, descriptor))
        }
    }
}

public class PlatformStaticAnnotationChecker : DeclarationChecker {

    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor.hasPlatformStaticAnnotation()) {
            if (declaration is JetNamedFunction || declaration is JetProperty || declaration is JetPropertyAccessor) {
                checkDeclaration(declaration, descriptor, diagnosticHolder)
            }
        }
    }

    private fun checkDeclaration(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink
    ) {
        val container = descriptor.getContainingDeclaration()
        val insideObject = container != null && DescriptorUtils.isNonCompanionObject(container)
        val insideCompanionObjectInClass =
                container != null && DescriptorUtils.isCompanionObject(container) &&
                container.getContainingDeclaration().let { DescriptorUtils.isClass(it) || DescriptorUtils.isEnumClass(it) }

        if (!insideObject && !insideCompanionObjectInClass) {
            diagnosticHolder.report(ErrorsJvm.JVM_STATIC_NOT_IN_OBJECT.on(declaration))
        }

        val checkDeclaration = when(declaration) {
            is JetPropertyAccessor -> declaration.getParent() as JetProperty
            else -> declaration
        }

        if (insideObject && checkDeclaration.getModifierList()?.hasModifier(JetTokens.OVERRIDE_KEYWORD) == true) {
            diagnosticHolder.report(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC.on(declaration))
        }
    }
}

public class JvmNameAnnotationChecker : DeclarationChecker {
    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink, bindingContext: BindingContext) {
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
            if (DescriptorUtils.isOverride(callableMemberDescriptor) || callableMemberDescriptor.modality.isOverridable) {
                diagnosticHolder.report(ErrorsJvm.INAPPLICABLE_JVM_NAME.on(annotationEntry))
            }
        }
    }

    private fun isRenamableFunction(descriptor: FunctionDescriptor): Boolean {
        val containingDescriptor = descriptor.containingDeclaration

        return containingDescriptor is PackageFragmentDescriptor || containingDescriptor is ClassDescriptor
    }
}

public class VolatileAnnotationChecker : DeclarationChecker {

    override fun check(declaration: JetDeclaration,
                       descriptor: DeclarationDescriptor,
                       diagnosticHolder: DiagnosticSink,
                       bindingContext: BindingContext
    ) {
        val volatileAnnotation = DescriptorUtils.getVolatileAnnotation(descriptor)
        if (volatileAnnotation != null && descriptor is PropertyDescriptor && !descriptor.isVar) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(volatileAnnotation) ?: return
            diagnosticHolder.report(ErrorsJvm.VOLATILE_ON_VALUE.on(annotationEntry))
        }
    }
}

public class SynchronizedAnnotationChecker : DeclarationChecker {

    override fun check(declaration: JetDeclaration,
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

public class OverloadsAnnotationChecker: DeclarationChecker {
    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor.hasJvmOverloadsAnnotation()) {
            checkDeclaration(declaration, descriptor, diagnosticHolder)
        }
    }

    private fun checkDeclaration(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor !is CallableDescriptor) {
            return
        }
        if (descriptor is FunctionDescriptor && descriptor.getModality() == Modality.ABSTRACT) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_ABSTRACT.on(declaration))
        }
        else if ((!descriptor.getVisibility().isPublicAPI && descriptor.getVisibility() != Visibilities.INTERNAL) ||
                 DescriptorUtils.isLocal(descriptor)) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_PRIVATE.on(declaration))
        }
        else if (descriptor.getValueParameters().none { it.declaresDefaultValue() }) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS.on(declaration))
        }
    }
}

public class TypeParameterBoundIsNotArrayChecker : DeclarationChecker {
    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        val typeParameters = (descriptor as? CallableDescriptor)?.typeParameters
                             ?: (descriptor as? ClassDescriptor)?.typeConstructor?.parameters
                             ?: return

        for (typeParameter in typeParameters) {
            if (typeParameter.upperBounds.any { KotlinBuiltIns.isArray(it) || KotlinBuiltIns.isPrimitiveArray(it) }) {
                val element = DescriptorToSourceUtils.descriptorToDeclaration(typeParameter) ?: declaration
                diagnosticHolder.report(ErrorsJvm.UPPER_BOUND_CANNOT_BE_ARRAY.on(element))
            }
        }
    }
}

public class ReifiedTypeParameterAnnotationChecker : DeclarationChecker {

    override fun check(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor.hasIntrinsicAnnotation()) return

        if (descriptor is CallableDescriptor && !descriptor.hasInlineAnnotation()) {
            checkTypeParameterDescriptorsAreNotReified(descriptor.getTypeParameters(), diagnosticHolder)
        }
        if (descriptor is ClassDescriptor) {
            checkTypeParameterDescriptorsAreNotReified(descriptor.getTypeConstructor().getParameters(), diagnosticHolder)
        }
    }


    private fun checkTypeParameterDescriptorsAreNotReified(
            typeParameterDescriptors: List<TypeParameterDescriptor>,
            diagnosticHolder: DiagnosticSink
    ) {
        for (reifiedTypeParameterDescriptor in typeParameterDescriptors.filter { it.isReified() }) {
            val typeParameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(reifiedTypeParameterDescriptor)
            if (typeParameterDeclaration !is JetTypeParameter) throw AssertionError("JetTypeParameter expected")

            diagnosticHolder.report(
                    Errors.REIFIED_TYPE_PARAMETER_NO_INLINE.on(
                            typeParameterDeclaration.getModifierList()!!.getModifier(JetTokens.REIFIED_KEYWORD)!!
                    )
            )
        }
    }
}