/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.kotlin

import org.jetbrains.jet.lang.resolve.AdditionalCheckerProvider
import org.jetbrains.jet.lang.resolve.AnnotationChecker
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.resolve.annotations.hasPlatformStaticAnnotation
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.resolve.java.diagnostics.ErrorsJvm
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.diagnostics.DiagnosticSink
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.resolve.annotations.hasInlineAnnotation
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import org.jetbrains.jet.lang.psi.JetTypeParameter
import org.jetbrains.jet.lang.resolve.annotations.hasIntrinsicAnnotation
import org.jetbrains.jet.lang.resolve.kotlin.nativeDeclarations.NativeFunChecker

public object JavaDeclarationCheckerProvider : AdditionalCheckerProvider {

    override val annotationCheckers: List<AnnotationChecker> = listOf(
            PlatformStaticAnnotationChecker(), LocalFunInlineChecker(), ReifiedTypeParameterAnnotationChecker(), NativeFunChecker()
    )
}

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
            if (declaration is JetNamedFunction || declaration is JetProperty) {
                checkDeclaration(declaration, descriptor, diagnosticHolder, declaration)
            }
            else {
                //TODO: there should be general mechanism
                diagnosticHolder.report(ErrorsJvm.PLATFORM_STATIC_ILLEGAL_USAGE.on(declaration, descriptor));
            }
        }

        if (declaration is JetProperty) {
            val getter = declaration.getGetter()
            if (getter != null) {
                val propertyGetterDescriptor = (descriptor as PropertyDescriptor).getGetter()!!
                if (propertyGetterDescriptor.hasPlatformStaticAnnotation()) {
                    checkDeclaration(declaration, descriptor, diagnosticHolder, getter)
                }
            }

            val setter = declaration.getSetter()
            if (setter != null) {
                val propertySetterDescriptor = (descriptor as PropertyDescriptor).getSetter()!!
                if (propertySetterDescriptor.hasPlatformStaticAnnotation()) {
                    checkDeclaration(declaration, descriptor, diagnosticHolder, setter)
                }
            }
        }
    }

    private fun checkDeclaration(
            declaration: JetDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            reportDiagnosticOn: JetDeclaration
    ) {
        val insideObject = containerKindIs(descriptor, ClassKind.OBJECT)
        val insideClassObject = containerKindIs(descriptor, ClassKind.CLASS_OBJECT)

        if (!insideObject && !(insideClassObject && containerKindIs(descriptor.getContainingDeclaration()!!, ClassKind.CLASS))) {
            diagnosticHolder.report(ErrorsJvm.PLATFORM_STATIC_NOT_IN_OBJECT.on(reportDiagnosticOn));
        }

        if (insideObject && declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) {
            diagnosticHolder.report(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC.on(reportDiagnosticOn));
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
