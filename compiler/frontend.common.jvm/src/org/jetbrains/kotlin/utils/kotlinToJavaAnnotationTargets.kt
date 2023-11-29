/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.lang.annotation.ElementType

val KOTLIN_TO_JAVA_ANNOTATION_TARGETS: Map<String, String> = mapOf(
    AnnotationTarget.CLASS.name to ElementType.TYPE.name,
    AnnotationTarget.ANNOTATION_CLASS.name to ElementType.ANNOTATION_TYPE.name,
    AnnotationTarget.FIELD.name to ElementType.FIELD.name,
    AnnotationTarget.LOCAL_VARIABLE.name to ElementType.LOCAL_VARIABLE.name,
    AnnotationTarget.VALUE_PARAMETER.name to ElementType.PARAMETER.name,
    AnnotationTarget.CONSTRUCTOR.name to ElementType.CONSTRUCTOR.name,
    AnnotationTarget.FUNCTION.name to ElementType.METHOD.name,
    AnnotationTarget.PROPERTY_GETTER.name to ElementType.METHOD.name,
    AnnotationTarget.PROPERTY_SETTER.name to ElementType.METHOD.name,
    AnnotationTarget.TYPE_PARAMETER.name to ElementType.TYPE_PARAMETER.name,
    AnnotationTarget.TYPE.name to ElementType.TYPE_USE.name,
)