/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.container.PlatformSpecificExtension
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun Deprecation.deprecatedByOverriddenMessage(): String? = (this as? DeprecatedByOverridden)?.additionalMessage()

fun Deprecation.deprecatedByAnnotationReplaceWithExpression(): String? = (this as? DeprecatedByAnnotation)?.replaceWithValue

// The function extracts value of warningSince/errorSince/hiddenSince from DeprecatedSinceKotlin annotation
fun AnnotationDescriptor.getSinceVersion(name: String): ApiVersion? =
    argumentValue(name)?.safeAs<StringValue>()?.value?.takeUnless(String::isEmpty)?.let(ApiVersion.Companion::parse)

fun computeLevelForDeprecatedSinceKotlin(annotation: AnnotationDescriptor, apiVersion: ApiVersion): DeprecationLevelValue? {
    val hiddenSince = annotation.getSinceVersion("hiddenSince")
    if (hiddenSince != null && apiVersion >= hiddenSince) return HIDDEN

    val errorSince = annotation.getSinceVersion("errorSince")
    if (errorSince != null && apiVersion >= errorSince) return ERROR

    val warningSince = annotation.getSinceVersion("warningSince")
    if (warningSince != null && apiVersion >= warningSince) return WARNING

    return null
}

internal fun createDeprecationDiagnostic(
    element: PsiElement, deprecation: Deprecation, languageVersionSettings: LanguageVersionSettings
): Diagnostic {
    val targetOriginal = deprecation.target.original
    return when (deprecation) {
        is DeprecatedByVersionRequirement -> {
            val factory = when (deprecation.deprecationLevel) {
                WARNING -> Errors.VERSION_REQUIREMENT_DEPRECATION
                ERROR, HIDDEN -> Errors.VERSION_REQUIREMENT_DEPRECATION_ERROR
            }
            factory.on(
                element, targetOriginal, deprecation.versionRequirement.version,
                languageVersionSettings.languageVersion to deprecation.message
            )
        }

        is DeprecatedTypealiasByAnnotation -> {
            val factory = when (deprecation.deprecationLevel) {
                WARNING -> Errors.TYPEALIAS_EXPANSION_DEPRECATION
                ERROR, HIDDEN -> Errors.TYPEALIAS_EXPANSION_DEPRECATION_ERROR
            }
            factory.on(element, deprecation.typeAliasTarget.original, deprecation.nested.target.original, deprecation.nested.message ?: "")
        }

        else -> {
            val factory = when (deprecation.deprecationLevel) {
                WARNING -> Errors.DEPRECATION
                ERROR, HIDDEN -> Errors.DEPRECATION_ERROR
            }
            factory.on(element, targetOriginal, deprecation.message ?: "")
        }
    }
}

@DefaultImplementation(DeprecationSettings.Default::class)
interface DeprecationSettings {
    fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor): Boolean

    object Default : DeprecationSettings {
        override fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor) = true
    }
}
