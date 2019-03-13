/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.container.PlatformExtensionsClashResolver
import org.jetbrains.kotlin.container.PlatformSpecificExtension
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun Deprecation.deprecatedByOverriddenMessage(): String? = (this as? DeprecatedByOverridden)?.additionalMessage()

fun Deprecation.deprecatedByAnnotationReplaceWithExpression(): String? {
    val annotation = (this as? DeprecatedByAnnotation)?.annotation ?: return null
    val replaceWithAnnotation =
        annotation.argumentValue(kotlin.Deprecated::replaceWith.name)?.safeAs<AnnotationValue>()?.value ?: return null
    return replaceWithAnnotation.argumentValue(kotlin.ReplaceWith::expression.name)?.safeAs<StringValue>()?.value
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

@DefaultImplementation(CoroutineCompatibilitySupport::class)
class CoroutineCompatibilitySupport private constructor(val enabled: Boolean) : PlatformSpecificExtension<CoroutineCompatibilitySupport>{
    @Suppress("unused")
    constructor() : this(true)

    companion object {
        val ENABLED = CoroutineCompatibilitySupport(true)

        val DISABLED = CoroutineCompatibilitySupport(false)
    }
}

class CoroutineCompatibilitySupportClashesResolver : PlatformExtensionsClashResolver.UseAnyOf<CoroutineCompatibilitySupport>(
    CoroutineCompatibilitySupport.DISABLED,
    CoroutineCompatibilitySupport::class.java
)

@DefaultImplementation(DeprecationSettings.Default::class)
interface DeprecationSettings {
    fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor): Boolean

    object Default : DeprecationSettings {
        override fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor) = true
    }
}
