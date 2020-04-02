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

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.RequireKotlinConstants
import org.jetbrains.kotlin.resolve.SINCE_KOTLIN_FQ_NAME
import org.jetbrains.kotlin.resolve.source.getPsi

abstract class KotlinVersionStringAnnotationValueChecker(
    private val annotationFqName: FqName
) : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val annotation = descriptor.annotations.findAnnotation(annotationFqName) ?: return
        val version = annotation.allValueArguments.values.singleOrNull()?.value as? String ?: return
        if (!version.matches(RequireKotlinConstants.VERSION_REGEX)) {
            context.trace.report(Errors.ILLEGAL_KOTLIN_VERSION_STRING_VALUE.on(annotation.source.getPsi() ?: declaration, annotationFqName))
            return
        }

        extraCheck(declaration, annotation, version, context.trace, context.languageVersionSettings)
    }

    open fun extraCheck(
        declaration: KtDeclaration,
        annotation: AnnotationDescriptor,
        version: String,
        diagnosticHolder: DiagnosticSink,
        languageVersionSettings: LanguageVersionSettings
    ) {
    }
}

object SinceKotlinAnnotationValueChecker : KotlinVersionStringAnnotationValueChecker(SINCE_KOTLIN_FQ_NAME) {
    override fun extraCheck(
        declaration: KtDeclaration,
        annotation: AnnotationDescriptor,
        version: String,
        diagnosticHolder: DiagnosticSink,
        languageVersionSettings: LanguageVersionSettings
    ) {
        val apiVersion = ApiVersion.parse(version)
        val specified = languageVersionSettings.apiVersion
        if (apiVersion != null && apiVersion > specified) {
            diagnosticHolder.report(
                Errors.NEWER_VERSION_IN_SINCE_KOTLIN.on(
                    annotation.source.getPsi() ?: declaration,
                    specified.versionString
                )
            )
        }
    }
}

object RequireKotlinAnnotationValueChecker : KotlinVersionStringAnnotationValueChecker(RequireKotlinConstants.FQ_NAME)
