/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DescriptorDerivedFromTypeAlias
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.SinceKotlinAccessibility
import org.jetbrains.kotlin.resolve.calls.checkers.isOperatorMod
import org.jetbrains.kotlin.resolve.calls.checkers.shouldWarnAboutDeprecatedModFromBuiltIns
import org.jetbrains.kotlin.resolve.checkSinceKotlinVersionAccessibility
import org.jetbrains.kotlin.resolve.checkers.OptInUsageChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.SmartList
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class DeprecationResolver(
    storageManager: StorageManager,
    private val languageVersionSettings: LanguageVersionSettings,
    private val deprecationSettings: DeprecationSettings
) {
    private val deprecations: MemoizedFunctionToNotNull<DeclarationDescriptor, DeprecationInfo> =
        storageManager.createMemoizedFunction { descriptor ->
            computeDeprecation(descriptor)
        }

    private fun computeDeprecation(descriptor: DeclarationDescriptor): DeprecationInfo {
        val deprecations = descriptor.getOwnDeprecations()
        return when {
            deprecations.isNotEmpty() -> DeprecationInfo(deprecations, hasInheritedDeprecations = false)
            descriptor is PropertyAccessorDescriptor && descriptor.correspondingProperty is SyntheticPropertyDescriptor -> {
                val syntheticProperty = descriptor.correspondingProperty as SyntheticPropertyDescriptor
                val originalMethod =
                    if (descriptor is PropertyGetterDescriptor) syntheticProperty.getMethod else syntheticProperty.setMethod

                @Suppress("FoldInitializerAndIfToElvis") // Wait until KTIJ-26450 is fixed
                if (originalMethod == null) return DeprecationInfo.EMPTY
                val originalMethodDeprecationInfo = deprecations(originalMethod)

                // Limiting these new (they didn't exist before 1.9.10) deprecations only to WARNING and forcePropagationToOverrides
                // (i.e., for overrides of NOT_CONSIDERED JDK members)
                // is deliberate once we would like to reduce the scope of affected usages because otherwise
                // it might be a big unexpected breaking change for users who are enabled -Werror flag.
                val filteredDeprecations =
                    originalMethodDeprecationInfo.deprecations.filter {
                        it.deprecationLevel == DeprecationLevelValue.WARNING && it.forcePropagationToOverrides
                    }
                return originalMethodDeprecationInfo.copy(deprecations = filteredDeprecations)
            }
            descriptor is CallableMemberDescriptor -> {
                val inheritedDeprecations = listOfNotNull(deprecationByOverridden(descriptor))
                when (inheritedDeprecations.isNotEmpty()) {
                    true -> when (languageVersionSettings.supportsFeature(LanguageFeature.StopPropagatingDeprecationThroughOverrides)) {
                        true -> DeprecationInfo(
                            inheritedDeprecations.filter { it.forcePropagationToOverrides },
                            hasInheritedDeprecations = true,
                            inheritedDeprecations
                        )
                        false -> DeprecationInfo(inheritedDeprecations, hasInheritedDeprecations = true)
                    }
                    false -> DeprecationInfo.EMPTY
                }
            }
            else -> DeprecationInfo.EMPTY
        }
    }

    private data class DeprecationInfo(
        val deprecations: List<DescriptorBasedDeprecationInfo>,
        val hasInheritedDeprecations: Boolean,
        val hiddenInheritedDeprecations: List<DescriptorBasedDeprecationInfo> = emptyList()
    ) {
        companion object {
            val EMPTY = DeprecationInfo(emptyList(), hasInheritedDeprecations = false, emptyList())
        }
    }

    private val isHiddenBecauseOfKotlinVersionAccessibility = storageManager.createMemoizedFunction { descriptor: DeclarationDescriptor ->
        descriptor.checkSinceKotlinVersionAccessibility(languageVersionSettings)
    }

    fun getDeprecations(descriptor: DeclarationDescriptor): List<DescriptorBasedDeprecationInfo> =
        deprecations(descriptor.original).deprecations

    @OptIn(ExperimentalContracts::class)
    fun areDeprecationsInheritedFromOverriden(descriptor: DeclarationDescriptor): Boolean {
        contract {
            returns(true) implies (descriptor is CallableMemberDescriptor)
        }
        return deprecations(descriptor.original).hasInheritedDeprecations
    }

    fun getHiddenDeprecationsFromOverriden(descriptor: DeclarationDescriptor): List<DescriptorBasedDeprecationInfo> =
        deprecations(descriptor.original).hiddenInheritedDeprecations

    fun isDeprecatedHidden(descriptor: DeclarationDescriptor): Boolean =
        getDeprecations(descriptor).any { it.deprecationLevel == DeprecationLevelValue.HIDDEN }

    @JvmOverloads
    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor,
        call: Call? = null,
        bindingContext: BindingContext? = null,
        isSuperCall: Boolean = false,
        fromImportingScope: Boolean = false
    ): Boolean =
        isHiddenInResolution(descriptor, call?.callElement, bindingContext, isSuperCall, fromImportingScope)

    fun isHiddenInResolution(
        descriptor: DeclarationDescriptor,
        callElement: KtElement?,
        bindingContext: BindingContext?,
        isSuperCall: Boolean,
        fromImportingScope: Boolean
    ): Boolean {
        if (descriptor is FunctionDescriptor) {
            if (descriptor.isHiddenToOvercomeSignatureClash) return true
            if (descriptor.isHiddenForResolutionEverywhereBesideSupercalls && !isSuperCall) return true
        }

        val sinceKotlinAccessibility = isHiddenBecauseOfKotlinVersionAccessibility(descriptor.original)
        if (sinceKotlinAccessibility is SinceKotlinAccessibility.NotAccessible) return true

        if (sinceKotlinAccessibility is SinceKotlinAccessibility.NotAccessibleButWasExperimental) {
            return if (callElement != null && bindingContext != null) {
                with(OptInUsageChecker) {
                    sinceKotlinAccessibility.markerClasses.any { classDescriptor ->
                        !callElement.isOptInAllowed(classDescriptor.fqNameSafe, languageVersionSettings, bindingContext)
                    }
                }
            } else {
                // We need a softer check for descriptors from importing scope as there is no access to PSI elements
                // It's fine to return false here as there will be additional checks for accessibility later
                !fromImportingScope
            }
        }

        return isDeprecatedHidden(descriptor)
    }

    private fun KotlinType.deprecationsByConstituentTypes(): List<DescriptorBasedDeprecationInfo> =
        SmartList<DescriptorBasedDeprecationInfo>().also { deprecations ->
            TypeUtils.contains(this) { type ->
                type.constructor.declarationDescriptor?.let {
                    deprecations.addAll(getDeprecations(it))
                }
                false
            }
        }

    private fun deprecationByOverridden(root: CallableMemberDescriptor): DescriptorBasedDeprecationInfo? {
        val visited = HashSet<CallableMemberDescriptor>()
        val deprecations = LinkedHashSet<DescriptorBasedDeprecationInfo>()
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

        if (deprecations.isEmpty()) return null
        if (hasUndeprecatedOverridden && deprecations.none { it.forcePropagationToOverrides }) return null

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
        if (root.kind.isReal && deprecations.none(DescriptorBasedDeprecationInfo::propagatesToOverrides)) return null

        return DeprecatedByOverridden(deprecations)
    }

    private fun DeclarationDescriptor.getOwnDeprecations(): List<DescriptorBasedDeprecationInfo> {
        // The problem is that declaration `mod` in built-ins has @Deprecated annotation but actually it was deprecated only in version 1.1
        if (isBuiltInOperatorMod && !shouldWarnAboutDeprecatedModFromBuiltIns(languageVersionSettings)) {
            return emptyList()
        }

        // This is a temporary workaround before @DeprecatedSinceKotlin is introduced, see KT-23575
        if (shouldSkipDeprecationOnKotlinIoReadBytes(this, languageVersionSettings)) {
            return emptyList()
        }

        val result = SmartList<DescriptorBasedDeprecationInfo>()

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

    private fun DeclarationDescriptor.addDeprecationIfPresent(result: MutableList<DescriptorBasedDeprecationInfo>) {
        val annotation = annotations.findAnnotation(StandardNames.FqNames.deprecated) ?: annotations.findAnnotation(JAVA_DEPRECATED)
        if (annotation != null) {
            val deprecatedByAnnotation =
                DeprecatedByAnnotation.create(
                    annotation, annotations.findAnnotation(StandardNames.FqNames.deprecatedSinceKotlin),
                    this, deprecationSettings.propagatedToOverrides(annotation),
                    languageVersionSettings.apiVersion
                )
            if (deprecatedByAnnotation != null) {
                val deprecation = when {
                    this is TypeAliasConstructorDescriptor ->
                        DeprecatedTypealiasByAnnotation(typeAliasDescriptor, deprecatedByAnnotation)

                    isBuiltInOperatorMod ->
                        DeprecatedOperatorMod(languageVersionSettings, deprecatedByAnnotation)

                    else -> deprecatedByAnnotation
                }
                result.add(deprecation)
            }
        }

        for (deprecation in getDeprecationByVersionRequirement(this)) {
            result.add(deprecation)
        }
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

    private fun getDeprecationFromUserData(target: DeclarationDescriptor): DescriptorBasedDeprecationInfo? =
        (target as? CallableDescriptor)?.getUserData(DEPRECATED_FUNCTION_KEY)

    private fun getDeprecationByVersionRequirement(target: DeclarationDescriptor): List<DeprecatedByVersionRequirement> {
        val versionRequirements =
            (target as? DeserializedMemberDescriptor)?.versionRequirements
                ?: (target as? DeserializedClassDescriptor)?.versionRequirements
                ?: return emptyList()

        return versionRequirements.mapNotNull { versionRequirement ->
            if (!versionRequirement.isFulfilled(this.languageVersionSettings))
                DeprecatedByVersionRequirement(versionRequirement, target)
            else
                null
        }
    }

    companion object {
        val JAVA_DEPRECATED = FqName("java.lang.Deprecated")
    }
}
