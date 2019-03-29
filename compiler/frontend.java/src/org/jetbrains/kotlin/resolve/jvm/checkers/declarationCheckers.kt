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
import org.jetbrains.kotlin.resolve.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.bytecodeVersion
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.calls.components.isActualParameterWithCorrespondingExpectedDefault
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotationConstructor
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.annotations.VOLATILE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.findSynchronizedAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.isInlineClassThatRequiresMangling
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameMangling

class LocalFunInlineChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (InlineUtil.isInline(descriptor) &&
            declaration is KtNamedFunction &&
            descriptor is FunctionDescriptor &&
            descriptor.visibility == Visibilities.LOCAL) {
            context.trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(declaration, "Local inline functions"))
        }
    }
}

class JvmStaticChecker(jvmTarget: JvmTarget, languageVersionSettings: LanguageVersionSettings) : DeclarationChecker {
    private val isLessJVM18 = jvmTarget.bytecodeVersion < JvmTarget.JVM_1_8.bytecodeVersion

    private val supportJvmStaticInInterface = languageVersionSettings.supportsFeature(LanguageFeature.JvmStaticInInterface)

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor.hasJvmStaticAnnotation()) {
            if (declaration is KtNamedFunction ||
                declaration is KtProperty ||
                declaration is KtPropertyAccessor ||
                declaration is KtParameter) {
                checkDeclaration(declaration, descriptor, context.trace)
            }
        }
    }

    private fun checkDeclaration(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        diagnosticHolder: DiagnosticSink
    ) {
        val container = descriptor.containingDeclaration
        val insideObject = DescriptorUtils.isObject(container)
        val insideCompanionObjectInInterface = DescriptorUtils.isCompanionObject(container) &&
                DescriptorUtils.isInterface(container!!.containingDeclaration)

        if (!insideObject || insideCompanionObjectInInterface) {
            if (insideCompanionObjectInInterface &&
                supportJvmStaticInInterface &&
                descriptor is DeclarationDescriptorWithVisibility) {
                checkVisibility(descriptor, diagnosticHolder, declaration)
                if (isLessJVM18) {
                    diagnosticHolder.report(ErrorsJvm.JVM_STATIC_IN_INTERFACE_1_6.on(declaration))
                }
            } else {
                diagnosticHolder.report(
                    (if (supportJvmStaticInInterface) ErrorsJvm.JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION
                    else ErrorsJvm.JVM_STATIC_NOT_IN_OBJECT_OR_CLASS_COMPANION).on(declaration)
                )
            }
        }

        val checkDeclaration = when (declaration) {
            is KtPropertyAccessor -> declaration.getParent() as KtProperty
            else -> declaration
        }

        if (DescriptorUtils.isNonCompanionObject(container) && checkDeclaration.modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) == true) {
            diagnosticHolder.report(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC.on(declaration))
        }

        if (descriptor is PropertyDescriptor && (descriptor.isConst || descriptor.hasJvmFieldAnnotation())) {
            diagnosticHolder.report(ErrorsJvm.JVM_STATIC_ON_CONST_OR_JVM_FIELD.on(declaration))
        }
    }

    private fun checkVisibility(
        descriptor: DeclarationDescriptorWithVisibility,
        diagnosticHolder: DiagnosticSink,
        declaration: KtDeclaration
    ) {
        if (descriptor.visibility != Visibilities.PUBLIC) {
            diagnosticHolder.report(ErrorsJvm.JVM_STATIC_ON_NON_PUBLIC_MEMBER.on(declaration))
        } else if (descriptor is PropertyDescriptor) {
            descriptor.setter?.let { checkVisibility(it, diagnosticHolder, declaration) }
        }
    }
}

class JvmNameAnnotationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val jvmNameAnnotation = DescriptorUtils.findJvmNameAnnotation(descriptor)
        if (jvmNameAnnotation != null) {
            checkDeclaration(descriptor, jvmNameAnnotation, context.trace)
        }
    }

    private fun checkDeclaration(descriptor: DeclarationDescriptor, annotation: AnnotationDescriptor, diagnosticHolder: DiagnosticSink) {
        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return

        if (descriptor is FunctionDescriptor && !isRenamableFunction(descriptor)) {
            diagnosticHolder.report(ErrorsJvm.INAPPLICABLE_JVM_NAME.on(annotationEntry))
        }

        val value = DescriptorUtils.getJvmName(descriptor)
        if (value == null || !Name.isValidIdentifier(value)) {
            diagnosticHolder.report(ErrorsJvm.ILLEGAL_JVM_NAME.on(annotationEntry))
        }

        if (descriptor is CallableMemberDescriptor) {
            if (DescriptorUtils.isOverride(descriptor) || descriptor.isOverridable) {
                diagnosticHolder.report(ErrorsJvm.INAPPLICABLE_JVM_NAME.on(annotationEntry))
            } else if (descriptor.containingDeclaration.isInlineClassThatRequiresMangling() ||
                requiresFunctionNameMangling(descriptor.valueParameters.map { it.type })
            ) {
                diagnosticHolder.report(ErrorsJvm.INAPPLICABLE_JVM_NAME.on(annotationEntry))
            }
        }
    }

    private fun isRenamableFunction(descriptor: FunctionDescriptor): Boolean {
        val containingDescriptor = descriptor.containingDeclaration

        return containingDescriptor is PackageFragmentDescriptor || containingDescriptor is ClassDescriptor
    }
}

class VolatileAnnotationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is PropertyDescriptor) return

        val fieldAnnotation = descriptor.backingField?.annotations?.findAnnotation(VOLATILE_ANNOTATION_FQ_NAME)
        if (fieldAnnotation != null && !descriptor.isVar) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(fieldAnnotation) ?: return
            context.trace.report(ErrorsJvm.VOLATILE_ON_VALUE.on(annotationEntry))
        }

        val delegateAnnotation = descriptor.delegateField?.annotations?.findAnnotation(VOLATILE_ANNOTATION_FQ_NAME)
        if (delegateAnnotation != null) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(delegateAnnotation) ?: return
            context.trace.report(ErrorsJvm.VOLATILE_ON_DELEGATE.on(annotationEntry))
        }
    }
}

class SynchronizedAnnotationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val synchronizedAnnotation = descriptor.findSynchronizedAnnotation()
        if (synchronizedAnnotation != null && descriptor is FunctionDescriptor && descriptor.modality == Modality.ABSTRACT) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(synchronizedAnnotation) ?: return
            context.trace.report(ErrorsJvm.SYNCHRONIZED_ON_ABSTRACT.on(annotationEntry))
        }
    }
}

class OverloadsAnnotationChecker: DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        descriptor.findJvmOverloadsAnnotation()?.let { annotation ->
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation)
            if (annotationEntry != null) {
                checkDeclaration(annotationEntry, descriptor, context)
            }
        }
    }

    private fun checkDeclaration(
        annotationEntry: KtAnnotationEntry,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        val diagnosticHolder = context.trace

        if (descriptor !is CallableDescriptor) {
            return
        } else if ((descriptor.containingDeclaration as? ClassDescriptor)?.kind == ClassKind.INTERFACE) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_INTERFACE.on(annotationEntry))
        } else if (descriptor is FunctionDescriptor && descriptor.modality == Modality.ABSTRACT) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_ABSTRACT.on(annotationEntry))
        } else if (DescriptorUtils.isLocal(descriptor)) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_LOCAL.on(annotationEntry))
        } else if (descriptor.isAnnotationConstructor()) {
            val diagnostic =
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses))
                    ErrorsJvm.OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR
                else
                    ErrorsJvm.OVERLOADS_ANNOTATION_CLASS_CONSTRUCTOR_WARNING

            diagnosticHolder.report(diagnostic.on(annotationEntry))
        } else if (!descriptor.visibility.isPublicAPI && descriptor.visibility != Visibilities.INTERNAL) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_PRIVATE.on(annotationEntry))
        } else if (descriptor.valueParameters.none { it.declaresDefaultValue() || it.isActualParameterWithCorrespondingExpectedDefault }) {
            diagnosticHolder.report(ErrorsJvm.OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS.on(annotationEntry))
        }
    }
}

class TypeParameterBoundIsNotArrayChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val typeParameters = (descriptor as? CallableDescriptor)?.typeParameters
                ?: (descriptor as? ClassDescriptor)?.declaredTypeParameters
                ?: return

        for (typeParameter in typeParameters) {
            if (typeParameter.upperBounds.any { KotlinBuiltIns.isArray(it) || KotlinBuiltIns.isPrimitiveArray(it) }) {
                val element = DescriptorToSourceUtils.descriptorToDeclaration(typeParameter) ?: declaration
                context.trace.report(ErrorsJvm.UPPER_BOUND_CANNOT_BE_ARRAY.on(element))
            }
        }
    }
}
