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

package org.jetbrains.jet.lang.resolve.kotlin.nativeDeclarations

import org.jetbrains.jet.lang.resolve.DiagnosticsWithSuppression
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.resolve.AnnotationChecker
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.diagnostics.DiagnosticSink
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor
import org.jetbrains.jet.lang.descriptors.Modality
import org.jetbrains.jet.lang.resolve.java.diagnostics.ErrorsJvm
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.lang.resolve.annotations.hasInlineAnnotation

private val NATIVE_ANNOTATION_CLASS_NAME = FqName("kotlin.jvm.native")

public fun DeclarationDescriptor.hasNativeAnnotation(): Boolean {
    return getAnnotations().findAnnotation(NATIVE_ANNOTATION_CLASS_NAME) != null
}

class SuppressNoBodyErrorsForNativeDeclarations : DiagnosticsWithSuppression.SuppressStringProvider {
    override fun get(annotationDescriptor: AnnotationDescriptor): List<String> {
        val descriptor = DescriptorUtils.getClassDescriptorForType(annotationDescriptor.getType())
        if (NATIVE_ANNOTATION_CLASS_NAME.asString() == DescriptorUtils.getFqName(descriptor).asString()) {
            return listOf(
                    Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY.getName().toLowerCase(),
                    Errors.NON_MEMBER_FUNCTION_NO_BODY.getName().toLowerCase(),
                    Errors.FINAL_FUNCTION_WITH_NO_BODY.getName().toLowerCase()
            )
        }

        return listOf()
    }
}

public class NativeFunChecker : AnnotationChecker {
    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        if (!descriptor.hasNativeAnnotation()) return

        if (DescriptorUtils.isTrait(descriptor.getContainingDeclaration())) {
            diagnosticHolder.report(ErrorsJvm.NATIVE_DECLARATION_IN_TRAIT.on(declaration))
        }
        else if (descriptor is CallableMemberDescriptor &&
            descriptor.getModality() == Modality.ABSTRACT) {
            diagnosticHolder.report(ErrorsJvm.NATIVE_DECLARATION_CANNOT_BE_ABSTRACT.on(declaration))
        }

        if (declaration is JetDeclarationWithBody && declaration.hasBody()) {
            diagnosticHolder.report(ErrorsJvm.NATIVE_DECLARATION_CANNOT_HAVE_BODY.on(declaration))
        }

        if (descriptor.hasInlineAnnotation()) {
            diagnosticHolder.report(ErrorsJvm.NATIVE_DECLARATION_CANNOT_BE_INLINED.on(declaration))
        }

    }
}
