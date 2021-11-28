/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.REPEATABLE_ANNOTATION
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.resolve.PlatformAnnotationFeaturesSupport
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention

class JvmPlatformAnnotationFeaturesSupport(
    private val languageVersionSettings: LanguageVersionSettings,
) : PlatformAnnotationFeaturesSupport {
    override fun isRepeatableAnnotationClass(descriptor: ClassDescriptor): Boolean {
        check(descriptor.kind == ClassKind.ANNOTATION_CLASS) { descriptor }

        // This service only handles annotation classes annotated with java.lang.annotation.Repeatable.
        if (!descriptor.annotations.hasAnnotation(REPEATABLE_ANNOTATION)) return false

        // Before 1.6, only Java annotations with SOURCE retention could be used as repeatable.
        // (Note that _Kotlin_ annotations must have been annotated with kotlin.annotation.Repeatable, not j.l.a.Repeatable!)
        return languageVersionSettings.supportsFeature(LanguageFeature.RepeatableAnnotations) ||
                descriptor.getAnnotationRetention() == KotlinRetention.SOURCE && descriptor is JavaClassDescriptor
    }
}
