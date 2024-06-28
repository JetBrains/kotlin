/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.BuiltInAnnotationDescriptor
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.checkers.shouldWarnAboutDeprecatedModFromBuiltIns
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

internal sealed class DeprecatedByAnnotation(
    val annotation: AnnotationDescriptor,
    override val target: DeclarationDescriptor,
    final override val propagatesToOverrides: Boolean,
    final override val forcePropagationToOverrides: Boolean = false,
) : DescriptorBasedDeprecationInfo() {

    init {
        require(!forcePropagationToOverrides || propagatesToOverrides) {
            "if something is `forcePropagationToOverrides`, it's expected that `propagatesToOverrides` == true, too"
        }
    }

    override val message: String?
        get() = (annotation.argumentValue("message") as? StringValue)?.value

    internal val replaceWithValue: String?
        get() {
            val replaceWithAnnotation = (annotation.argumentValue(Deprecated::replaceWith.name) as? AnnotationValue)?.value
            return (replaceWithAnnotation?.argumentValue(ReplaceWith::expression.name) as? StringValue)?.value
        }

    class StandardDeprecated(
        annotation: AnnotationDescriptor,
        target: DeclarationDescriptor,
        propagatesToOverrides: Boolean,
        forcePropagationToOverrides: Boolean = false,
    ) : DeprecatedByAnnotation(annotation, target, propagatesToOverrides, forcePropagationToOverrides) {
        override val deprecationLevel: DeprecationLevelValue
            get() = when ((annotation.argumentValue("level") as? EnumValue)?.enumEntryName?.asString()) {
                "WARNING" -> WARNING
                "ERROR" -> ERROR
                "HIDDEN" -> HIDDEN
                else -> WARNING
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StandardDeprecated) return false

            if (annotation != other.annotation) return false
            if (target != other.target) return false
            if (propagatesToOverrides != other.propagatesToOverrides) return false

            return true
        }

        override fun hashCode(): Int {
            var hash = annotation.hashCode()
            hash = hash * 31 + target.hashCode()
            hash = hash * 31 + propagatesToOverrides.hashCode()
            return hash
        }
    }

    class DeprecatedSince(
        annotation: AnnotationDescriptor,
        target: DeclarationDescriptor,
        propagatesToOverrides: Boolean,
        override val deprecationLevel: DeprecationLevelValue
    ) : DeprecatedByAnnotation(annotation, target, propagatesToOverrides) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DeprecatedSince) return false

            if (annotation != other.annotation) return false
            if (target != other.target) return false
            if (propagatesToOverrides != other.propagatesToOverrides) return false
            if (deprecationLevel != other.deprecationLevel) return false

            return true
        }

        override fun hashCode(): Int {
            var hash = annotation.hashCode()
            hash = hash * 31 + target.hashCode()
            hash = hash * 31 + propagatesToOverrides.hashCode()
            hash = hash * 31 + deprecationLevel.hashCode()
            return hash
        }
    }

    companion object {
        fun create(
            deprecatedAnnotation: AnnotationDescriptor,
            deprecatedSinceKotlinAnnotation: AnnotationDescriptor?,
            target: DeclarationDescriptor,
            propagatesToOverrides: Boolean,
            apiVersion: ApiVersion
        ): DeprecatedByAnnotation? {
            if (deprecatedSinceKotlinAnnotation != null) {
                val level = computeLevelForDeprecatedSinceKotlin(deprecatedSinceKotlinAnnotation, apiVersion) ?: return null
                return DeprecatedSince(deprecatedAnnotation, target, propagatesToOverrides, level)
            }
            val forcePropagationToOverrides =
                (deprecatedAnnotation as? BuiltInAnnotationDescriptor)?.forcePropagationDeprecationToOverrides == true
            return StandardDeprecated(
                deprecatedAnnotation,
                target,
                propagatesToOverrides || forcePropagationToOverrides,
                forcePropagationToOverrides = forcePropagationToOverrides
            )
        }
    }
}

internal data class DeprecatedByOverridden(private val deprecations: Collection<DescriptorBasedDeprecationInfo>) : DescriptorBasedDeprecationInfo() {
    init {
        assert(deprecations.isNotEmpty())
        assert(deprecations.none { it is DeprecatedByOverridden })
    }

    override val forcePropagationToOverrides: Boolean = deprecations.any { it.forcePropagationToOverrides }

    override val deprecationLevel: DeprecationLevelValue = deprecations.map(DescriptorBasedDeprecationInfo::deprecationLevel).minOrNull()!!

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

internal data class DeprecatedOperatorMod(
    val languageVersionSettings: LanguageVersionSettings,
    val currentDeprecation: DescriptorBasedDeprecationInfo
) : DescriptorBasedDeprecationInfo() {
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
) : DescriptorBasedDeprecationInfo() {
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
) : DescriptorBasedDeprecationInfo() {
    override val target get() = typeAliasTarget
    override val deprecationLevel get() = nested.deprecationLevel
    override val message get() = nested.message
}
