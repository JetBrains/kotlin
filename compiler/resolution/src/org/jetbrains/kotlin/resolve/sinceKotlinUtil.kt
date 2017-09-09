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

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForTypeAliasObject

private val SINCE_KOTLIN_FQ_NAME = FqName("kotlin.SinceKotlin")

// TODO: use-site targeted annotations
fun DeclarationDescriptor.getSinceKotlinAnnotation(): AnnotationDescriptor? =
        annotations.findAnnotation(SINCE_KOTLIN_FQ_NAME)

/**
 * @return true if the descriptor is accessible according to [languageVersionSettings], or false otherwise. The [actionIfInaccessible]
 * callback is called with the version specified in the [SinceKotlin] annotation if the descriptor is inaccessible.
 */
fun DeclarationDescriptor.checkSinceKotlinVersionAccessibility(
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

    val typeAliasDescriptor = (this as? TypeAliasDescriptor) ?:
                              (this as? TypeAliasConstructorDescriptor)?.typeAliasDescriptor ?:
                              (this as? FakeCallableDescriptorForTypeAliasObject)?.typeAliasDescriptor

    val typeAlias = typeAliasDescriptor?.loadAnnotationValue()

    // We should check only the upper-most classifier ('A' in 'A<B<C>>') to guarantee binary compatibility.
    val underlyingClass = typeAliasDescriptor?.classDescriptor?.loadAnnotationValue()

    val underlyingConstructor = (this as? TypeAliasConstructorDescriptor)?.underlyingConstructorDescriptor?.loadAnnotationValue()
    val underlyingObject = (this as? FakeCallableDescriptorForTypeAliasObject)?.getReferencedObject()?.loadAnnotationValue()

    return listOfNotNull(ownVersion, ctorClass, property, typeAlias, underlyingClass, underlyingConstructor, underlyingObject).max()
}
