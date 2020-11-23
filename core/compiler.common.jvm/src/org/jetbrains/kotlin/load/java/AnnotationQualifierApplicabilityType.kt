/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

enum class AnnotationQualifierApplicabilityType {
    METHOD_RETURN_TYPE, VALUE_PARAMETER, FIELD, TYPE_USE, TYPE_PARAMETER_BOUNDS
}
