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

package org.jetbrains.kotlin.resolve.diagnostics

import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty

val FUNCTION_NO_BODY_ERRORS: List<DiagnosticFactory1<KtFunction, SimpleFunctionDescriptor>> =
        listOf(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, Errors.NON_MEMBER_FUNCTION_NO_BODY)

val PROPERTY_NOT_INITIALIZED_ERRORS: List<DiagnosticFactory0<KtProperty>> =
        listOf(Errors.MUST_BE_INITIALIZED, Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT)

abstract class SuppressDiagnosticsByAnnotations(
        diagnosticsToSuppress: List<DiagnosticFactory<out Diagnostic>>,
        vararg annotationFqNames: FqName
) : SuppressStringProvider {

    private val stringsToSuppress = diagnosticsToSuppress.map { it.name.toLowerCase() }
    private val expectedFqNames = annotationFqNames.toSet()

    override fun get(annotationDescriptor: AnnotationDescriptor): List<String> {
        return if (annotationDescriptor.fqName in expectedFqNames) stringsToSuppress else emptyList()
    }
}
