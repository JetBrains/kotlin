/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

enum class ForbiddenNamedArgumentsTarget(private val description: String) {
    NON_KOTLIN_FUNCTION("non-Kotlin functions"),  // a function provided by non-Kotlin artifact, ex: Java function
    INVOKE_ON_FUNCTION_TYPE("function types"),
    EXPECTED_CLASS_MEMBER("members of expected classes"),
    // TODO: add the following when MPP support is available
//     INTEROP_FUNCTION("interop functions with ambiguous parameter names"),  // deserialized Kotlin function that serves as a bridge to a function written in another language, ex: Obj-C
    ;

    override fun toString(): String = description
}
