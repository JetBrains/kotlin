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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.resolve.annotations.hasPlatformStaticAnnotation
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.resolve.annotations.hasInlineAnnotation
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.psi.JetTypeParameter
import org.jetbrains.kotlin.resolve.annotations.hasIntrinsicAnnotation
import org.jetbrains.kotlin.load.kotlin.nativeDeclarations.NativeFunChecker
import org.jetbrains.kotlin.psi.JetPropertyAccessor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.resolve.jvm.calls.checkers.NeedSyntheticChecker

public object KotlinJvmCheckerProvider : AdditionalCheckerProvider(
        annotationCheckers = listOf(PlatformStaticAnnotationChecker(), LocalFunInlineChecker(), ReifiedTypeParameterAnnotationChecker(), NativeFunChecker()),
        additionalCallCheckers = listOf(NeedSyntheticChecker())
)

public class LocalFunInlineChecker : AnnotationChecker {

    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor.hasInlineAnnotation() &&
            declaration is JetNamedFunction &&
            descriptor is FunctionDescriptor &&
            descriptor.getVisibility() == Visibilities.LOCAL) {
            diagnosticHolder.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(declaration, declaration, descriptor))
        }
    }
}

public class PlatformStaticAnnotationChecker : AnnotationChecker {

    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor.hasPlatformStaticAnnotation()) {
            if (declaration is JetNamedFunction || declaration is JetProperty || declaration is JetPropertyAccessor) {
                checkDeclaration(declaration, descriptor, diagnosticHolder)
            }
            else {
                //TODO: there should be general mechanism
                diagnosticHolder.report(ErrorsJvm.PLATFORM_STATIC_ILLEGAL_USAGE.on(declaration, descriptor));
            }
        }
    }

    private fun checkDeclaration(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink
    ) {
        val insideObject = containerKindIs(descriptor, ClassKind.OBJECT)
        val insideClassObject = containerKindIs(descriptor, ClassKind.CLASS_OBJECT)

        if (!insideObject && !(insideClassObject && containerKindIs(descriptor.getContainingDeclaration()!!, ClassKind.CLASS))) {
            diagnosticHolder.report(ErrorsJvm.PLATFORM_STATIC_NOT_IN_OBJECT.on(declaration));
        }

        if (insideObject && descriptor is MemberDescriptor && descriptor.getModality().isOverridable()) {
            diagnosticHolder.report(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC.on(declaration));
        }
    }

    private fun containerKindIs(descriptor: DeclarationDescriptor, kind: ClassKind): Boolean {
        val parentDeclaration = descriptor.getContainingDeclaration()
        return parentDeclaration != null && DescriptorUtils.isKindOf(parentDeclaration, kind)
    }
}

public class ReifiedTypeParameterAnnotationChecker : AnnotationChecker {

    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (descriptor.hasIntrinsicAnnotation()) return

        if (descriptor is CallableDescriptor && !descriptor.hasInlineAnnotation()) {
            checkTypeParameterDescriptorsAreNotReified(descriptor.getTypeParameters(), diagnosticHolder)
        }
        if (descriptor is ClassDescriptor) {
            checkTypeParameterDescriptorsAreNotReified(descriptor.getTypeConstructor().getParameters(), diagnosticHolder)
        }
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
                        typeParameterDeclaration.getModifierList().getModifier(JetTokens.REIFIED_KEYWORD)!!
                )
        )
    }
}
