/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.resolve.deprecation.DeprecationSettings

object JavaDeprecationSettings : DeprecationSettings {
    override fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor): Boolean {
        if (deprecationAnnotation is JavaDeprecatedAnnotationDescriptor) return false
        return true
    }
}
