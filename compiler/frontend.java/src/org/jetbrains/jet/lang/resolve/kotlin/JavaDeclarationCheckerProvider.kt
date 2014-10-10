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
import org.jetbrains.jet.lang.descriptors.MemberDescriptor
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
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor

public object JavaDeclarationCheckerProvider : AdditionalCheckerProvider {

    override val annotationCheckers: List<AnnotationChecker> = listOf(PlatformStaticAnnotationChecker(), LocalFunInlineChecker())
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
            if (declaration is JetNamedFunction) {
                val insideObject = DescriptorUtils.containerKindIs(descriptor, ClassKind.OBJECT)
                val insideClassObject = DescriptorUtils.containerKindIs(descriptor, ClassKind.CLASS_OBJECT)

                if (!insideObject && !(insideClassObject && DescriptorUtils.containerKindIs(descriptor.getContainingDeclaration()!!, ClassKind.CLASS))) {
                    diagnosticHolder.report(ErrorsJvm.PLATFORM_STATIC_NOT_IN_OBJECT.on(declaration, descriptor));
                }

                if (insideObject && declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) {
                    diagnosticHolder.report(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC.on(declaration, descriptor));
                }
            } else {
                //TODO: there should be general mechanism
                diagnosticHolder.report(ErrorsJvm.PLATFORM_STATIC_ILLEGAL_USAGE.on(declaration, descriptor));
            }
        }

        if (declaration is JetProperty) {
            val getter = declaration.getGetter()
            if (getter != null) {
                check(getter, (descriptor as PropertyDescriptor).getGetter()!!, diagnosticHolder)
            }
            val setter = declaration.getSetter()
            if (setter != null) {
                check(setter, (descriptor as PropertyDescriptor).getSetter()!!, diagnosticHolder)
            }
        }
    }
}