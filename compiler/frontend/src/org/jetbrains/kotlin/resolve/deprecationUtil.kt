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

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.DescriptorDerivedFromTypeAlias
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.DeprecationLevelValue.*
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.checkers.isOperatorMod
import org.jetbrains.kotlin.resolve.calls.checkers.shouldWarnAboutDeprecatedModFromBuiltIns
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor.CoroutinesCompatibilityMode.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val JAVA_DEPRECATED = FqName("java.lang.Deprecated")

interface Deprecation {
    val deprecationLevel: DeprecationLevelValue
    val message: String?
    val target: DeclarationDescriptor
}

fun Deprecation.deprecatedByOverriddenMessage(): String? = (this as? DeprecatedByOverridden)?.additionalMessage()

fun Deprecation.deprecatedByAnnotationReplaceWithExpression(): String? {
    val annotation = (this as? DeprecatedByAnnotation)?.annotation ?: return null
    val replaceWithAnnotation =
        annotation.argumentValue(kotlin.Deprecated::replaceWith.name)?.safeAs<AnnotationValue>()?.value ?: return null
    return replaceWithAnnotation.argumentValue(kotlin.ReplaceWith::expression.name)?.safeAs<StringValue>()?.value
}

private data class DeprecatedByAnnotation(val annotation: AnnotationDescriptor, override val target: DeclarationDescriptor) : Deprecation {
    override val deprecationLevel: DeprecationLevelValue
        get() = when (annotation.argumentValue("level")?.safeAs<EnumValue>()?.enumEntryName?.asString()) {
            "WARNING" -> WARNING
            "ERROR" -> ERROR
            "HIDDEN" -> HIDDEN
            else -> WARNING
        }

    override val message: String?
        get() = annotation.argumentValue("message")?.safeAs<StringValue>()?.value
}

private data class DeprecatedByOverridden(private val deprecations: Collection<Deprecation>) : Deprecation {
    init {
        assert(deprecations.isNotEmpty())
        assert(deprecations.none {
            it is DeprecatedByOverridden
        })
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

private data class DeprecatedExperimentalCoroutine(
    override val target: DeclarationDescriptor,
    override val deprecationLevel: DeprecationLevelValue
) : Deprecation {
    override val message: String? =
        if (deprecationLevel == WARNING)
            "Experimental coroutines support will be dropped in 1.4"
        else
            "Experimental coroutine cannot be used with API version 1.3"
}

private data class DeprecatedByVersionRequirement(
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

private data class DeprecatedTypealiasByAnnotation(
    val typeAliasTarget: TypeAliasDescriptor,
    val nested: DeprecatedByAnnotation
) : Deprecation {
    override val target get() = typeAliasTarget
    override val deprecationLevel get() = nested.deprecationLevel
    override val message get() = nested.message
}

private fun Deprecation.wrapInTypeAliasExpansion(typeAliasDescriptor: TypeAliasDescriptor) = when {
    this is DeprecatedByAnnotation -> DeprecatedTypealiasByAnnotation(typeAliasDescriptor, this)
    else -> this
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

// values from kotlin.DeprecationLevel
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}

class DeprecationResolver(
    storageManager: StorageManager,
    private val languageVersionSettings: LanguageVersionSettings
) {
    private val deprecations = storageManager.createMemoizedFunction { descriptor: DeclarationDescriptor ->
        val deprecations = descriptor.getOwnDeprecations()
        when {
            deprecations.isNotEmpty() -> deprecations
            descriptor is CallableMemberDescriptor -> listOfNotNull(deprecationByOverridden(descriptor))
            else -> emptyList()
        }
    }

    private val isHiddenBecauseOfKotlinVersionAccessibility = storageManager.createMemoizedFunction { descriptor: DeclarationDescriptor ->
        descriptor.checkSinceKotlinVersionAccessibility(languageVersionSettings)
    }

    fun getDeprecations(descriptor: DeclarationDescriptor): List<Deprecation> =
        deprecations(descriptor.original)

    fun isDeprecatedHidden(descriptor: DeclarationDescriptor): Boolean =
        getDeprecations(descriptor).any { it.deprecationLevel == HIDDEN }

    @JvmOverloads
    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor,
        call: Call? = null,
        bindingContext: BindingContext? = null,
        isSuperCall: Boolean = false
    ): Boolean {
        if (descriptor is FunctionDescriptor) {
            if (descriptor.isHiddenToOvercomeSignatureClash) return true
            if (descriptor.isHiddenForResolutionEverywhereBesideSupercalls && !isSuperCall) return true
        }

        val sinceKotlinAccessibility = isHiddenBecauseOfKotlinVersionAccessibility(descriptor.original)
        if (sinceKotlinAccessibility is SinceKotlinAccessibility.NotAccessible) return true

        if (sinceKotlinAccessibility is SinceKotlinAccessibility.NotAccessibleButWasExperimental) {
            if (call != null && bindingContext != null) {
                return with(ExperimentalUsageChecker) {
                    sinceKotlinAccessibility.markerClasses.any { classDescriptor ->
                        !call.callElement.isExperimentalityAccepted(classDescriptor.fqNameSafe, languageVersionSettings, bindingContext)
                    }
                }
            }
            return true
        }

        return isDeprecatedHidden(descriptor)
    }

    private fun KotlinType.deprecationsByConstituentTypes(): List<Deprecation> =
        SmartList<Deprecation>().also { deprecations ->
            TypeUtils.contains(this) { type ->
                type.constructor.declarationDescriptor?.let {
                    deprecations.addAll(getDeprecations(it))
                }
                false
            }
        }

    private fun deprecationByOverridden(root: CallableMemberDescriptor): Deprecation? {
        val visited = HashSet<CallableMemberDescriptor>()
        val deprecations = LinkedHashSet<Deprecation>()
        var hasUndeprecatedOverridden = false

        fun traverse(node: CallableMemberDescriptor) {
            if (node in visited) return

            visited.add(node)

            val deprecationsByAnnotation = node.getOwnDeprecations()
            val overriddenDescriptors = node.original.overriddenDescriptors
            when {
                deprecationsByAnnotation.isNotEmpty() -> {
                    deprecations.addAll(deprecationsByAnnotation)
                }
                overriddenDescriptors.isEmpty() -> {
                    hasUndeprecatedOverridden = true
                    return
                }
                else -> {
                    overriddenDescriptors.forEach(::traverse)
                }
            }
        }

        traverse(root)

        if (hasUndeprecatedOverridden || deprecations.isEmpty()) return null

        return DeprecatedByOverridden(deprecations)
    }

    private fun DeclarationDescriptor.getOwnDeprecations(): List<Deprecation> {
        // The problem is that declaration `mod` in built-ins has @Deprecated annotation but actually it was deprecated only in version 1.1
        if (this is FunctionDescriptor && this.isOperatorMod() && KotlinBuiltIns.isUnderKotlinPackage(this)) {
            if (!shouldWarnAboutDeprecatedModFromBuiltIns(languageVersionSettings)) {
                return emptyList()
            }
        }

        val result = SmartList<Deprecation>()

        fun addDeprecationIfPresent(target: DeclarationDescriptor) {
            val annotation = target.annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.deprecated)
                ?: target.annotations.findAnnotation(JAVA_DEPRECATED)
            if (annotation != null) {
                val deprecatedByAnnotation = DeprecatedByAnnotation(annotation, target)
                val deprecation = when (target) {
                    is TypeAliasConstructorDescriptor -> DeprecatedTypealiasByAnnotation(target.typeAliasDescriptor, deprecatedByAnnotation)
                    else -> deprecatedByAnnotation
                }
                result.add(deprecation)
            }

            for (deprecation in getDeprecationByVersionRequirement(target)) {
                result.add(deprecation)
            }
            getDeprecationByCoroutinesVersion(target)?.let(result::add)
        }

        fun addUseSiteTargetedDeprecationIfPresent(annotatedDescriptor: DeclarationDescriptor, useSiteTarget: AnnotationUseSiteTarget?) {
            if (useSiteTarget != null) {
                val annotation = Annotations.findUseSiteTargetedAnnotation(
                    annotatedDescriptor.annotations,
                    useSiteTarget,
                    KotlinBuiltIns.FQ_NAMES.deprecated
                )
                    ?: Annotations.findUseSiteTargetedAnnotation(annotatedDescriptor.annotations, useSiteTarget, JAVA_DEPRECATED)
                if (annotation != null) {
                    result.add(DeprecatedByAnnotation(annotation, this))
                }
            }
        }

        addDeprecationIfPresent(this)
        addUseSiteTargetedDeprecationIfPresent(this, AnnotationUseSiteTarget.getAssociatedUseSiteTarget(this))

        when (this) {
            is TypeAliasDescriptor -> {
                expandedType.deprecationsByConstituentTypes().mapTo(result) { it.wrapInTypeAliasExpansion(this) }
            }
            is DescriptorDerivedFromTypeAlias -> {
                result.addAll(typeAliasDescriptor.getOwnDeprecations())
            }
            is PropertyAccessorDescriptor -> {
                addDeprecationIfPresent(correspondingProperty)

                addUseSiteTargetedDeprecationIfPresent(
                    correspondingProperty,
                    if (this is PropertyGetterDescriptor) AnnotationUseSiteTarget.PROPERTY_GETTER else AnnotationUseSiteTarget.PROPERTY_SETTER
                )
            }
        }

        return result.distinct()
    }

    private fun getDeprecationByCoroutinesVersion(target: DeclarationDescriptor): DeprecatedExperimentalCoroutine? {
        if (target !is DeserializedMemberDescriptor) return null
        return when (target.coroutinesExperimentalCompatibilityMode) {
            COMPATIBLE -> null
            NEEDS_WRAPPER -> DeprecatedExperimentalCoroutine(target, WARNING)
            INCOMPATIBLE -> DeprecatedExperimentalCoroutine(target, ERROR)
        }
    }

    private fun getDeprecationByVersionRequirement(target: DeclarationDescriptor): List<DeprecatedByVersionRequirement> {
        fun createVersion(version: String): MavenComparableVersion? = try {
            MavenComparableVersion(version)
        } catch (e: Exception) {
            null
        }

        val versionRequirements =
            (target as? DeserializedMemberDescriptor)?.versionRequirements
                ?: (target as? DeserializedClassDescriptor)?.versionRequirements
                ?: return emptyList()

        return versionRequirements.mapNotNull { versionRequirement ->
            val requiredVersion = createVersion(versionRequirement.version.asString())
            val currentVersion = when (versionRequirement.kind) {
                ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION ->
                    MavenComparableVersion(languageVersionSettings.languageVersion.versionString)
                ProtoBuf.VersionRequirement.VersionKind.API_VERSION ->
                    languageVersionSettings.apiVersion.version
                ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION ->
                    KotlinCompilerVersion.getVersion()?.substringBefore('-')?.let(::createVersion)
                else -> null
            }
            if (currentVersion != null && currentVersion < requiredVersion)
                DeprecatedByVersionRequirement(versionRequirement, target)
            else
                null
        }
    }
}
