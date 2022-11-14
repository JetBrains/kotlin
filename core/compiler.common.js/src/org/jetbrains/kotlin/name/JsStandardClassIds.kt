/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.name.StandardClassIds.BASE_KOTLIN_PACKAGE

object JsStandardClassIds {
    val BASE_JS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("js"))

    object Annotations {
        val JsQualifier = "JsQualifier".jsId()
        val JsModule = "JsModule".jsId()
        val JsNonModule = "JsNonModule".jsId()
    }
}

private fun String.jsId() = ClassId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
