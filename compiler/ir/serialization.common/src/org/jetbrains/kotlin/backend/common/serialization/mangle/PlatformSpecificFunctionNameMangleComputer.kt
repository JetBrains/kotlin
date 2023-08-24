/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

interface PlatformSpecificFunctionNameMangleComputer<in ValueParameter : Any> {

    fun computePlatformSpecificFunctionName(): String?

    fun computePlatformSpecificValueParameterPrefix(valueParameter: ValueParameter): String

    object Default : PlatformSpecificFunctionNameMangleComputer<Any> {

        override fun computePlatformSpecificFunctionName(): String? = null

        override fun computePlatformSpecificValueParameterPrefix(valueParameter: Any): String = ""
    }
}