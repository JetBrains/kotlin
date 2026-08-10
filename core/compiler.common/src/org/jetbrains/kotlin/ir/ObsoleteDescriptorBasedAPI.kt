/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

/**
 * This annotation is used on IR API elements which use front-end (FE) descriptors to obtain information, directly or indirectly.
 *
 * Descriptors are used in FE to represent main declaration properties and to refer declarations.
 * Early IR versions were descriptor-based, so IR elements used descriptors to obtain some information about element properties.
 * However, more correct and universal way is to store all necessary information inside IR elements themselves
 * and do not use descriptors as some intermediate storage. It's planned to remove all descriptor usages from IR in future.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS
)
@RequiresOptIn(message = "Please use IR declaration properties and not its descriptor properties", level = RequiresOptIn.Level.ERROR)
annotation class ObsoleteDescriptorBasedAPI
