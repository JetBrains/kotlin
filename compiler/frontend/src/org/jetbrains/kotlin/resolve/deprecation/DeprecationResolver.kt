/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.SinceKotlinAccessibility
import org.jetbrains.kotlin.resolve.checkSinceKotlinVersionAccessibility
import org.jetbrains.kotlin.resolve.checkers.OptInUsageChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.storage.StorageManager
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@K1Deprecation
class DeprecationResolver(
    storageManager: StorageManager,
    languageVersionSettings: LanguageVersionSettings,
    deprecationSettings: DeprecationSettings
) : DeprecationResolverBase(storageManager, languageVersionSettings, deprecationSettings) {

    private val isHiddenBecauseOfKotlinVersionAccessibility = storageManager.createMemoizedFunction { descriptor: DeclarationDescriptor ->
        descriptor.checkSinceKotlinVersionAccessibility(languageVersionSettings)
    }

    @OptIn(ExperimentalContracts::class)
    fun areDeprecationsInheritedFromOverriden(descriptor: DeclarationDescriptor): Boolean {
        contract {
            returns(true) implies (descriptor is CallableMemberDescriptor)
        }
        return deprecations(descriptor.original).hasInheritedDeprecations
    }

    fun getHiddenDeprecationsFromOverriden(descriptor: DeclarationDescriptor): List<DescriptorBasedDeprecationInfo> =
        deprecations(descriptor.original).hiddenInheritedDeprecations

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

        if (!isDeprecatedHidden(descriptor)) return false
        // Here we would like to consider List.getFirst(Last) not as hidden but just as deprecated. See KT-66768.
        // setHiddenForResolutionEverywhereBesideSupercalls() (see JvmBuiltInsCustomizer.kt) does not work here,
        // because it e.g. makes overridden functions also hidden`(and we don't want it per KT-65441 decision).
        return !isSuperCall || descriptor.fqNameOrNull() !in KOTLIN_LIST_FIRST_LAST
    }

    companion object {
        val JAVA_DEPRECATED get() = DeprecationResolverBase.JAVA_DEPRECATED

        val LIST_DEPRECATED_PROPERTIES get() = DeprecationResolverBase.LIST_DEPRECATED_PROPERTIES

        val KOTLIN_LIST_FIRST_LAST = LIST_DEPRECATED_PROPERTIES.map { propertyName ->
            StandardNames.FqNames.list.child(Name.identifier("get${propertyName.replaceFirstChar { it.uppercase() }}"))
        }
    }
}
