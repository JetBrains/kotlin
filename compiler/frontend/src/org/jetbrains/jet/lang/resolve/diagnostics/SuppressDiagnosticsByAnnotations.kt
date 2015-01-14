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

package org.jetbrains.jet.lang.resolve.diagnostics

import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.JetFunction
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory1
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory0

public val FUNCTION_NO_BODY_ERRORS: List<DiagnosticFactory1<JetFunction, SimpleFunctionDescriptor>> =
        listOf(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, Errors.NON_MEMBER_FUNCTION_NO_BODY, Errors.FINAL_FUNCTION_WITH_NO_BODY)

public val PROPERTY_NOT_INITIALIZED_ERRORS: List<DiagnosticFactory0<JetProperty>> =
        listOf(Errors.MUST_BE_INITIALIZED, Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT)

public abstract class SuppressDiagnosticsByAnnotations(
        diagnosticsToSuppress: List<DiagnosticFactory<out Diagnostic>>,
        vararg annotationsFqName: FqName
) : DiagnosticsWithSuppression.SuppressStringProvider {

    private val annotationsFqName = annotationsFqName
    private val stringsToSuppress = diagnosticsToSuppress.map { it.getName().toLowerCase() }
    private val expectedFqNames = annotationsFqName.map { it.toString() }

    override fun get(annotationDescriptor: AnnotationDescriptor): List<String> {
        val descriptor = DescriptorUtils.getClassDescriptorForType(annotationDescriptor.getType())
        val actualFqName = DescriptorUtils.getFqName(descriptor).asString()

        if (expectedFqNames.any { it == actualFqName }) return stringsToSuppress

        return listOf()
    }
}
