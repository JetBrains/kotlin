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

        val JsNative = "native".jsId()
        val JsLibrary = "library".jsId()
        val JsNativeInvoke = "nativeInvoke".jsId()
        val JsNativeGetter = "nativeGetter".jsId()
        val JsNativeSetter = "nativeSetter".jsId()
        val JsName = "JsName".jsId()
    }

    object Callables {
        val JsDefinedExternally = "definedExternally".callableId(BASE_JS_PACKAGE)
        val JsNoImpl = "noImpl".callableId(BASE_JS_PACKAGE)
    }
}

private fun String.jsId() = ClassId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
