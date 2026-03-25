/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationUseSiteTarget
import org.jetbrains.kotlin.analysis.api.components.KaDeprecation
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

@KaImplementationDetail
fun KaAnnotationUseSiteTarget.toCompilerTarget(): AnnotationUseSiteTarget {
    return when (this) {
        KaAnnotationUseSiteTarget.ALL -> AnnotationUseSiteTarget.ALL
        KaAnnotationUseSiteTarget.FIELD -> AnnotationUseSiteTarget.FIELD
        KaAnnotationUseSiteTarget.FILE -> AnnotationUseSiteTarget.FILE
        KaAnnotationUseSiteTarget.PROPERTY -> AnnotationUseSiteTarget.PROPERTY
        KaAnnotationUseSiteTarget.PROPERTY_GETTER -> AnnotationUseSiteTarget.PROPERTY_GETTER
        KaAnnotationUseSiteTarget.PROPERTY_SETTER -> AnnotationUseSiteTarget.PROPERTY_SETTER
        KaAnnotationUseSiteTarget.RECEIVER -> AnnotationUseSiteTarget.RECEIVER
        KaAnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
        KaAnnotationUseSiteTarget.SETTER_PARAMETER -> AnnotationUseSiteTarget.SETTER_PARAMETER
        KaAnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
    }
}

@KaImplementationDetail
fun DeprecationLevelValue.toKaLevel(): KaDeprecation.Level {
    return when (this) {
        DeprecationLevelValue.WARNING -> KaDeprecation.Level.WARNING
        DeprecationLevelValue.ERROR -> KaDeprecation.Level.ERROR
        DeprecationLevelValue.HIDDEN -> KaDeprecation.Level.HIDDEN
    }
}
