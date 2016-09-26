/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName

private val SINCE_KOTLIN_FQ_NAME = FqName("kotlin.SinceKotlin")

// TODO: use-site targeted annotations
internal fun DeclarationDescriptor.getSinceKotlinAnnotation(): AnnotationDescriptor? =
        annotations.findAnnotation(SINCE_KOTLIN_FQ_NAME)

/**
 * @return true if the descriptor is accessible according to [languageVersionSettings], or false otherwise. The [actionIfInaccessible]
 * callback is called with the version specified in the [SinceKotlin] annotation if the descriptor is inaccessible.
 */
internal fun DeclarationDescriptor.checkSinceKotlinVersionAccessibility(
        languageVersionSettings: LanguageVersionSettings,
        actionIfInaccessible: ((ApiVersion) -> Unit)? = null
): Boolean {
    val version =
            if (this is CallableMemberDescriptor && !kind.isReal) getSinceKotlinVersionByOverridden(this)
            else getOwnSinceKotlinVersion()

    // Allow access in the following cases:
    // 1) There's no @SinceKotlin annotation for this descriptor
    // 2) There's a @SinceKotlin annotation but its value is some unrecognizable nonsense
    // 3) The value as a version is not greater than our API version
    if (version == null || version <= languageVersionSettings.apiVersion) return true

    actionIfInaccessible?.invoke(version)

    return false
}

/**
 * @return null if there are no overridden members or if there's at least one declaration in the hierarchy not annotated with [SinceKotlin],
 *         or the minimal value of the version from all declarations annotated with [SinceKotlin] otherwise.
 */
private fun getSinceKotlinVersionByOverridden(descriptor: CallableMemberDescriptor): ApiVersion? {
    return DescriptorUtils.getAllOverriddenDeclarations(descriptor).map { it.getOwnSinceKotlinVersion() ?: return null }.min()
}

private fun DeclarationDescriptor.getOwnSinceKotlinVersion(): ApiVersion? {
    fun DeclarationDescriptor.loadAnnotationValue(): ApiVersion? =
            (getSinceKotlinAnnotation()?.allValueArguments?.values?.singleOrNull()?.value as? String)?.let(ApiVersion.Companion::parse)

    val ownVersion = loadAnnotationValue()
    val ctorClass = (this as? ConstructorDescriptor)?.containingDeclaration?.loadAnnotationValue()
    val property = (this as? PropertyAccessorDescriptor)?.correspondingProperty?.loadAnnotationValue()

    return listOfNotNull(ownVersion, ctorClass, property).max()
}
