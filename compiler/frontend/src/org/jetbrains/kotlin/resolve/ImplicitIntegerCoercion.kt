/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName

object ImplicitIntegerCoercion {

    val MODULE_CAPABILITY = ModuleCapability<Boolean>("ImplicitIntegerCoercion")

    fun isEnabledFor(descriptor: DeclarationDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean =
        descriptor.hasImplicitIntegerCoercionAnnotation() ||
                (languageVersionSettings.supportsFeature(LanguageFeature.ImplicitSignedToUnsignedIntegerConversion) &&
                        DescriptorUtils.getContainingModuleOrNull(descriptor)?.hasImplicitIntegerCoercionCapability() == true)

    private val IMPLICIT_INTEGER_COERCION_ANNOTATION_FQ_NAME = FqName("kotlin.internal.ImplicitIntegerCoercion")

    private fun DeclarationDescriptor.hasImplicitIntegerCoercionAnnotation(): Boolean {
        return annotations.findAnnotation(IMPLICIT_INTEGER_COERCION_ANNOTATION_FQ_NAME) != null
    }
}

fun ModuleDescriptor.hasImplicitIntegerCoercionCapability(): Boolean {
    return getCapability(ImplicitIntegerCoercion.MODULE_CAPABILITY) == true
}
