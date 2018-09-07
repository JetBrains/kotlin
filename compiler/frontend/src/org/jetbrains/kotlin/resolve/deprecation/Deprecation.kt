/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.checkers.shouldWarnAboutDeprecatedModFromBuiltIns
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal sealed class DeprecatedByAnnotation(
    val annotation: AnnotationDescriptor,
    override val target: DeclarationDescriptor,
    override val propagatesToOverrides: Boolean
) : Deprecation {
    override val message: String?
        get() = annotation.argumentValue("message")?.safeAs<StringValue>()?.value

    internal val replaceWithValue: String?
        get() {
            val replaceWithAnnotation = annotation.argumentValue(Deprecated::replaceWith.name)?.safeAs<AnnotationValue>()?.value
            return replaceWithAnnotation?.argumentValue(ReplaceWith::expression.name)?.safeAs<StringValue>()?.value
        }

    class StandardDeprecated(
        annotation: AnnotationDescriptor,
        target: DeclarationDescriptor,
        propagatesToOverrides: Boolean
    ) : DeprecatedByAnnotation(annotation, target, propagatesToOverrides) {
        override val deprecationLevel: DeprecationLevelValue
            get() = when (annotation.argumentValue("level")?.safeAs<EnumValue>()?.enumEntryName?.asString()) {
                "WARNING" -> WARNING
                "ERROR" -> ERROR
                "HIDDEN" -> HIDDEN
                else -> WARNING
            }
    }

    class DeprecatedSince(
        annotation: AnnotationDescriptor,
        target: DeclarationDescriptor,
        propagatesToOverrides: Boolean,
        override val deprecationLevel: DeprecationLevelValue
    ) : DeprecatedByAnnotation(annotation, target, propagatesToOverrides)

    companion object {
        fun create(
            annotation: AnnotationDescriptor,
            target: DeclarationDescriptor,
            propagatesToOverrides: Boolean,
            apiVersion: ApiVersion
        ): DeprecatedByAnnotation? {
            if (annotation.fqName == KotlinBuiltIns.FQ_NAMES.deprecatedSinceKotlin) {
                val level = computeLevelForDeprecatedSinceKotlin(annotation, apiVersion) ?: return null
                return DeprecatedSince(annotation, target, propagatesToOverrides, level)
            }
            return StandardDeprecated(annotation, target, propagatesToOverrides)
        }

        private fun computeLevelForDeprecatedSinceKotlin(annotation: AnnotationDescriptor, apiVersion: ApiVersion): DeprecationLevelValue? {
            fun getSinceVersion(name: String): ApiVersion? =
                annotation.argumentValue(name)?.safeAs<StringValue>()?.value?.takeUnless(String::isEmpty)?.let(ApiVersion.Companion::parse)

            val hiddenSince = getSinceVersion("hiddenSince")
            if (hiddenSince != null && apiVersion >= hiddenSince) return HIDDEN

            val errorSince = getSinceVersion("errorSince")
            if (errorSince != null && apiVersion >= errorSince) return ERROR

            val warningSince = getSinceVersion("warningSince")
            if (warningSince != null && apiVersion >= warningSince) return WARNING

            return null
        }
    }
}

internal data class DeprecatedByOverridden(private val deprecations: Collection<Deprecation>) : Deprecation {
    init {
        assert(deprecations.isNotEmpty())
        assert(deprecations.none { it is DeprecatedByOverridden })
    }

    override val deprecationLevel: DeprecationLevelValue = deprecations.map(Deprecation::deprecationLevel).min()!!

    override val target: DeclarationDescriptor
        get() = deprecations.first().target

    override val message: String
        get() {
            val message = deprecations.filter { it.deprecationLevel == this.deprecationLevel }.map { it.message }.toSet().joinToString(". ")
            return "${additionalMessage()}. $message"
        }

    internal fun additionalMessage() =
        "Overrides deprecated member in '${DescriptorUtils.getContainingClass(target)!!.fqNameSafe.asString()}'"
}

internal data class DeprecatedExperimentalCoroutine(
    override val target: DeclarationDescriptor,
    override val deprecationLevel: DeprecationLevelValue
) : Deprecation {
    override val message: String? =
        if (deprecationLevel == WARNING)
            "Experimental coroutines support will be dropped in 1.4"
        else
            "Experimental coroutine cannot be used with API version 1.3"
}

internal data class DeprecatedOperatorMod(
    val languageVersionSettings: LanguageVersionSettings,
    val currentDeprecation: Deprecation
) : Deprecation {
    init {
        assert(shouldWarnAboutDeprecatedModFromBuiltIns(languageVersionSettings)) {
            "Deprecation created for mod that shouldn't have any deprecations; languageVersionSettings: $languageVersionSettings"
        }
    }

    override val deprecationLevel: DeprecationLevelValue
        get() = when (languageVersionSettings.apiVersion) {
            ApiVersion.KOTLIN_1_1, ApiVersion.KOTLIN_1_2 -> WARNING
            ApiVersion.KOTLIN_1_3 -> ERROR
            else -> ERROR
        }

    override val message: String?
        get() = currentDeprecation.message

    override val target: DeclarationDescriptor
        get() = currentDeprecation.target
}

internal data class DeprecatedByVersionRequirement(
    val versionRequirement: VersionRequirement,
    override val target: DeclarationDescriptor
) : Deprecation {
    override val deprecationLevel: DeprecationLevelValue
        get() = when (versionRequirement.level) {
            DeprecationLevel.WARNING -> WARNING
            DeprecationLevel.ERROR -> ERROR
            DeprecationLevel.HIDDEN -> HIDDEN
        }

    override val message: String?
        get() {
            val message = versionRequirement.message
            val errorCode = versionRequirement.errorCode
            if (message == null && errorCode == null) return null

            return buildString {
                if (message != null) {
                    append(message)
                    if (errorCode != null) {
                        append(" (error code $errorCode)")
                    }
                } else {
                    append("Error code $errorCode")
                }
            }
        }
}

internal data class DeprecatedTypealiasByAnnotation(
    val typeAliasTarget: TypeAliasDescriptor,
    val nested: DeprecatedByAnnotation
) : Deprecation {
    override val target get() = typeAliasTarget
    override val deprecationLevel get() = nested.deprecationLevel
    override val message get() = nested.message
}
