/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors.ILLEGAL_SINCE_KOTLIN_VALUE
import org.jetbrains.kotlin.diagnostics.Errors.NEWER_VERSION_IN_SINCE_KOTLIN
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.getSinceKotlinAnnotation
import org.jetbrains.kotlin.resolve.source.getPsi

object SinceKotlinAnnotationValueChecker : DeclarationChecker {
    private val regex: Regex = "(0|[1-9][0-9]*)".let { number -> Regex("$number\\.$number(\\.$number)?") }

    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
    ) {
        val annotation = descriptor.getSinceKotlinAnnotation() ?: return
        val version = annotation.allValueArguments.values.singleOrNull()?.value as? String ?: return
        if (!version.matches(regex)) {
            diagnosticHolder.report(ILLEGAL_SINCE_KOTLIN_VALUE.on(annotation.source.getPsi() ?: declaration))
            return
        }

        val apiVersion = ApiVersion.parse(version)
        val specified = languageVersionSettings.apiVersion
        if (apiVersion != null && apiVersion > specified) {
            diagnosticHolder.report(NEWER_VERSION_IN_SINCE_KOTLIN.on(annotation.source.getPsi() ?: declaration, specified.versionString))
        }
    }
}
