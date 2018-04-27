/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForTypeAliasObject
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker

sealed class SinceKotlinAccessibility {
    object Accessible : SinceKotlinAccessibility()

    data class NotAccessibleButWasExperimental(
        val version: ApiVersion,
        val annotationFqNames: List<FqName>
    ) : SinceKotlinAccessibility()

    data class NotAccessible(
        val version: ApiVersion
    ) : SinceKotlinAccessibility()
}

fun DeclarationDescriptor.checkSinceKotlinVersionAccessibility(languageVersionSettings: LanguageVersionSettings): SinceKotlinAccessibility {
    val version =
        if (this is CallableMemberDescriptor && !kind.isReal) getSinceKotlinVersionByOverridden(this)
        else getOwnSinceKotlinVersion()

    // Allow access in the following cases:
    // 1) There's no @SinceKotlin annotation for this descriptor
    // 2) There's a @SinceKotlin annotation but its value is some unrecognizable nonsense
    // 3) The value as a version is not greater than our API version
    if (version == null || version <= languageVersionSettings.apiVersion) return SinceKotlinAccessibility.Accessible

    val wasExperimentalFqNames = with(ExperimentalUsageChecker) { loadWasExperimentalMarkerFqNames() }
    if (wasExperimentalFqNames.isNotEmpty()) {
        return SinceKotlinAccessibility.NotAccessibleButWasExperimental(version, wasExperimentalFqNames)
    }

    return SinceKotlinAccessibility.NotAccessible(version)
}

/**
 * @return null if there are no overridden members or if there's at least one declaration in the hierarchy not annotated with [SinceKotlin],
 *         or the minimal value of the version from all declarations annotated with [SinceKotlin] otherwise.
 */
private fun getSinceKotlinVersionByOverridden(descriptor: CallableMemberDescriptor): ApiVersion? {
    return DescriptorUtils.getAllOverriddenDeclarations(descriptor).map { it.getOwnSinceKotlinVersion() ?: return null }.min()
}

private fun DeclarationDescriptor.getOwnSinceKotlinVersion(): ApiVersion? {
    // TODO: use-site targeted annotations
    fun DeclarationDescriptor.loadAnnotationValue(): ApiVersion? =
        (annotations.findAnnotation(SINCE_KOTLIN_FQ_NAME)?.allValueArguments?.values?.singleOrNull()?.value as? String)
            ?.let(ApiVersion.Companion::parse)

    val ownVersion = loadAnnotationValue()
    val ctorClass = (this as? ConstructorDescriptor)?.containingDeclaration?.loadAnnotationValue()
    val property = (this as? PropertyAccessorDescriptor)?.correspondingProperty?.loadAnnotationValue()

    val typeAliasDescriptor = (this as? TypeAliasDescriptor) ?: (this as? TypeAliasConstructorDescriptor)?.typeAliasDescriptor
    ?: (this as? FakeCallableDescriptorForTypeAliasObject)?.typeAliasDescriptor

    val typeAlias = typeAliasDescriptor?.loadAnnotationValue()

    // We should check only the upper-most classifier ('A' in 'A<B<C>>') to guarantee binary compatibility.
    val underlyingClass = typeAliasDescriptor?.classDescriptor?.loadAnnotationValue()

    val underlyingConstructor = (this as? TypeAliasConstructorDescriptor)?.underlyingConstructorDescriptor?.loadAnnotationValue()
    val underlyingObject = (this as? FakeCallableDescriptorForTypeAliasObject)?.getReferencedObject()?.loadAnnotationValue()

    return listOfNotNull(ownVersion, ctorClass, property, typeAlias, underlyingClass, underlyingConstructor, underlyingObject).max()
}
