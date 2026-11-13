/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.deprecation

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue

// The function extracts value of warningSince/errorSince/hiddenSince from DeprecatedSinceKotlin annotation
@K1Deprecation
fun AnnotationDescriptor.getSinceVersion(name: String): ApiVersion? =
    (argumentValue(name) as? StringValue)?.value?.takeUnless(String::isEmpty)?.let(ApiVersion.Companion::parse)


@K1Deprecation
fun computeLevelForDeprecatedSinceKotlin(annotation: AnnotationDescriptor, apiVersion: ApiVersion): DeprecationLevelValue? {
    val hiddenSince = annotation.getSinceVersion("hiddenSince")
    if (hiddenSince != null && apiVersion >= hiddenSince) return HIDDEN

    val errorSince = annotation.getSinceVersion("errorSince")
    if (errorSince != null && apiVersion >= errorSince) return ERROR

    val warningSince = annotation.getSinceVersion("warningSince")
    if (warningSince != null && apiVersion >= warningSince) return WARNING

    return null
}

@DefaultImplementation(DeprecationSettings.Default::class)
@K1Deprecation
interface DeprecationSettings {
    fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor): Boolean

    object Default : DeprecationSettings {
        override fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor) = true
    }
}
