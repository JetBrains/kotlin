/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.name.StandardClassIds.BASE_KOTLIN_PACKAGE

object JsStandardClassIds {
    val BASE_JS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("js"))

    object Annotations {
        @JvmField
        val JsQualifier = "JsQualifier".jsId()

        @JvmField
        val JsModule = "JsModule".jsId()

        @JvmField
        val JsNonModule = "JsNonModule".jsId()

        @JvmField
        val JsNative = "native".jsId()

        @JvmField
        val JsLibrary = "library".jsId()

        @JvmField
        val JsNativeInvoke = "nativeInvoke".jsId()

        @JvmField
        val JsNativeGetter = "nativeGetter".jsId()

        @JvmField
        val JsNativeSetter = "nativeSetter".jsId()

        @JvmField
        val JsName = "JsName".jsId()

        @JvmField
        val JsExport = "JsExport".jsId()

        @JvmField
        val JsExportIgnore = JsExport.createNestedClassId(Name.identifier("Ignore"))

        @JvmField
        val annotationsRequiringExternal = setOf(JsModule, JsQualifier)

        @JvmField
        val nativeAnnotations = setOf(JsNative, JsNativeInvoke, JsNativeGetter, JsNativeSetter)
    }

    object Callables {
        @JvmField
        val JsDefinedExternally = "definedExternally".callableId(BASE_JS_PACKAGE)

        @JvmField
        val JsNoImpl = "noImpl".callableId(BASE_JS_PACKAGE)

        @JvmField
        val definedExternallyPropertyNames = setOf(JsNoImpl, JsDefinedExternally)
    }
}

private fun String.jsId() = ClassId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
