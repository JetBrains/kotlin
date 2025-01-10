/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.name.StandardClassIds.BASE_KOTLIN_PACKAGE
import org.jetbrains.kotlin.name.StandardClassIds.BASE_REFLECT_PACKAGE

object JsStandardClassIds {
    val BASE_JS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("js"))
    val BASE_JS_INTERNAL_PACKAGE = BASE_JS_PACKAGE.child(Name.identifier("internal"))
    val BASE_REFLECT_JS_INTERNAL_PACKAGE = BASE_REFLECT_PACKAGE.child(Name.identifier("js")).child(Name.identifier("internal"))

    @JvmField
    val Promise = "Promise".jsId()

    @JvmField
    val JsObject = "JsObject".jsId()

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
        val JsImplicitExport = "JsImplicitExport".jsId()

        @JvmField
        val JsNoDispatchReceiver = "JsNoDispatchReceiver".jsId()

        @JvmField
        val JsStatic = "JsStatic".jsId()

        @JvmField
        val JsExternalInheritorsOnly = "JsExternalInheritorsOnly".jsId()

        @JvmField
        val JsExternalArgument = "JsExternalArgument".jsId()

        @JvmField
        val JsExportIgnore = JsExport.createNestedClassId(Name.identifier("Ignore"))

        @JvmField
        val JsFun = "JsFun".jsId()

        @JvmField
        val JsOutlinedFunction = "JsOutlinedFunction".jsId()

        @JvmField
        val JsGenerator = "JsGenerator".jsId()

        @JvmField
        val DoNotIntrinsify = "DoNotIntrinsify".jsId()

        @JvmField
        val annotationsRequiringExternal = setOf(JsModule, JsQualifier)

        @JvmField
        val nativeAnnotations = setOf(JsNative, JsNativeInvoke, JsNativeGetter, JsNativeSetter)
    }

    object Callables {
        @JvmField
        val JsCode = "js".callableId(BASE_JS_PACKAGE)

        @JvmField
        val JsDefinedExternally = "definedExternally".callableId(BASE_JS_PACKAGE)
    }
}

private fun String.jsId() = ClassId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
