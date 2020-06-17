/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.deprecation.getSinceVersion
import org.jetbrains.kotlin.resolve.source.getPsi

object DeprecatedSinceKotlinAnnotationChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val deprecatedSinceAnnotation = descriptor.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.deprecatedSinceKotlin) ?: return
        val deprecatedSinceAnnotationPsi = deprecatedSinceAnnotation.source.getPsi() as? KtAnnotationEntry ?: return

        val deprecatedAnnotation = descriptor.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.deprecated)

        val deprecatedSinceAnnotationName = deprecatedSinceAnnotationPsi.typeReference ?: return

        if (deprecatedAnnotation == null) {
            context.trace.report(
                Errors.DEPRECATED_SINCE_KOTLIN_WITHOUT_DEPRECATED.on(
                    deprecatedSinceAnnotationName
                )
            )
            return
        }

        if (deprecatedAnnotation.argumentValue(Deprecated::level.name) != null) {
            context.trace.report(
                Errors.DEPRECATED_SINCE_KOTLIN_WITH_DEPRECATED_LEVEL.on(
                    deprecatedSinceAnnotationName
                )
            )
            return
        }

        val warningSince = deprecatedSinceAnnotation.getSinceVersion("warningSince")
        val errorSince = deprecatedSinceAnnotation.getSinceVersion("errorSince")
        val hiddenSince = deprecatedSinceAnnotation.getSinceVersion("hiddenSince")
        if (!lessOrNull(warningSince, errorSince) || !lessOrNull(errorSince, hiddenSince) || !lessOrNull(warningSince, hiddenSince)) {
            context.trace.report(
                Errors.DEPRECATED_SINCE_KOTLIN_WITH_UNORDERED_VERSIONS.on(
                    deprecatedSinceAnnotationName
                )
            )
            return
        }
    }

    private fun lessOrNull(a: ApiVersion?, b: ApiVersion?): Boolean =
        if (a == null || b == null) true else a <= b
}
