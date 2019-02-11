/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DescriptorDerivedFromTypeAlias
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.SinceKotlinAccessibility
import org.jetbrains.kotlin.resolve.calls.checkers.isOperatorMod
import org.jetbrains.kotlin.resolve.calls.checkers.shouldWarnAboutDeprecatedModFromBuiltIns
import org.jetbrains.kotlin.resolve.checkSinceKotlinVersionAccessibility
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor.CoroutinesCompatibilityMode.COMPATIBLE
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor.CoroutinesCompatibilityMode.NEEDS_WRAPPER
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DeprecationResolver(
    storageManager: StorageManager,
    private val languageVersionSettings: LanguageVersionSettings,
    private val coroutineCompatibilitySupport: CoroutineCompatibilitySupport,
    private val deprecationSettings: DeprecationSettings
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
        getDeprecations(descriptor).any { it.deprecationLevel == DeprecationLevelValue.HIDDEN }

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

        // We might've filtered out not-propagating deprecations already in the initializer of `deprecationsByAnnotation` in the code above.
        // But it would lead to treating Java overridden as not-deprecated at all that works controversially in case of mixed J/K override:
        // interface J {
        //      @Deprecated
        //      void foo();
        // }
        //
        // interface K {
        //      @Deprecated("")
        //      fun foo();
        // }
        //
        // class K1 : K, J {
        //      // We'd probably better treating it as deprecated
        //      // Basically, it's just a corner case and we may change the behavior if it's too annoying
        //      override fun foo() {}
        // }
        //
        // Also, we don't ignore non-propagating deprecations in case of fake overrides
        // Because we don't want to depend on the choice of the base descriptor
        if (root.kind.isReal && deprecations.none(Deprecation::propagatesToOverrides)) return null

        return DeprecatedByOverridden(deprecations)
    }

    private fun DeclarationDescriptor.getOwnDeprecations(): List<Deprecation> {
        // The problem is that declaration `mod` in built-ins has @Deprecated annotation but actually it was deprecated only in version 1.1
        if (isBuiltInOperatorMod && !shouldWarnAboutDeprecatedModFromBuiltIns(languageVersionSettings)) {
            return emptyList()
        }

        // This is a temporary workaround before @DeprecatedSinceKotlin is introduced, see KT-23575
        if (shouldSkipDeprecationOnKotlinIoReadBytes(this, languageVersionSettings)) {
            return emptyList()
        }

        val result = SmartList<Deprecation>()

        addDeprecationIfPresent(result)

        when (this) {
            is TypeAliasDescriptor -> expandedType.deprecationsByConstituentTypes().mapTo(result) { deprecation ->
                when (deprecation) {
                    is DeprecatedByAnnotation -> DeprecatedTypealiasByAnnotation(this, deprecation)
                    else -> deprecation
                }
            }

            is DescriptorDerivedFromTypeAlias ->
                result.addAll(typeAliasDescriptor.getOwnDeprecations())

            is PropertyAccessorDescriptor ->
                correspondingProperty.addDeprecationIfPresent(result)
        }

        return result.distinct()
    }

    private fun DeclarationDescriptor.addDeprecationIfPresent(result: MutableList<Deprecation>) {
        val annotation = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.deprecated)
            ?: annotations.findAnnotation(JAVA_DEPRECATED)
        if (annotation != null) {
            val deprecatedByAnnotation =
                DeprecatedByAnnotation(
                    annotation, this,
                    deprecationSettings.propagatedToOverrides(annotation)
                )
            val deprecation = when {
                this is TypeAliasConstructorDescriptor ->
                    DeprecatedTypealiasByAnnotation(typeAliasDescriptor, deprecatedByAnnotation)

                isBuiltInOperatorMod ->
                    DeprecatedOperatorMod(languageVersionSettings, deprecatedByAnnotation)

                else -> deprecatedByAnnotation
            }
            result.add(deprecation)
        }

        for (deprecation in getDeprecationByVersionRequirement(this)) {
            result.add(deprecation)
        }
        getDeprecationByCoroutinesVersion(this)?.let(result::add)
        getDeprecationFromUserData(this)?.let(result::add)
    }

    private val DeclarationDescriptor.isBuiltInOperatorMod: Boolean
        get() = this is FunctionDescriptor && this.isOperatorMod() && KotlinBuiltIns.isUnderKotlinPackage(this)

    private fun shouldSkipDeprecationOnKotlinIoReadBytes(
        descriptor: DeclarationDescriptor, languageVersionSettings: LanguageVersionSettings
    ): Boolean =
        descriptor.name.asString() == "readBytes" &&
                (descriptor.containingDeclaration as? PackageFragmentDescriptor)?.fqName?.asString() == "kotlin.io" &&
                descriptor is FunctionDescriptor &&
                descriptor.valueParameters.singleOrNull()?.type?.let(KotlinBuiltIns::isInt) == true &&
                languageVersionSettings.apiVersion < ApiVersion.KOTLIN_1_3

    private fun getDeprecationByCoroutinesVersion(target: DeclarationDescriptor): DeprecatedExperimentalCoroutine? {
        if (target !is DeserializedMemberDescriptor) return null

        target.coroutinesExperimentalCompatibilityMode.let { mode ->
            return when {
                mode == COMPATIBLE -> null
                mode == NEEDS_WRAPPER && coroutineCompatibilitySupport.enabled ->
                    DeprecatedExperimentalCoroutine(target, DeprecationLevelValue.WARNING)
                else -> DeprecatedExperimentalCoroutine(target, DeprecationLevelValue.ERROR)
            }
        }
    }

    private fun getDeprecationFromUserData(target: DeclarationDescriptor): Deprecation? =
        target.safeAs<CallableDescriptor>()?.getUserData(DEPRECATED_FUNCTION_KEY)

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

    companion object {
        private val JAVA_DEPRECATED = FqName("java.lang.Deprecated")
    }
}
