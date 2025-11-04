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
    val BOXED_LONG_PACKAGE = BASE_JS_INTERNAL_PACKAGE.child(Name.identifier("boxedLong"))
    val LONG_AS_BIGINT_PACKAGE = BASE_JS_INTERNAL_PACKAGE.child(Name.identifier("longAsBigInt"))

    @JvmField
    val Promise = "Promise".jsId()

    @JvmField
    val JsObject = "JsObject".jsId()

    @JvmField
    val Date = "Date".jsId()

    @file:JvmField
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

        val JsExport = "JsExport".jsId()

        val JsImplicitExport = "JsImplicitExport".jsId()

        val JsNoDispatchReceiver = "JsNoDispatchReceiver".jsId()

        val JsStatic = "JsStatic".jsId()

        val JsExternalInheritorsOnly = "JsExternalInheritorsOnly".jsId()

        val JsExternalArgument = "JsExternalArgument".jsId()

        val JsExportIgnore = JsExport.createNestedClassId(Name.identifier("Ignore"))

        val JsExportDefault = JsExport.createNestedClassId(Name.identifier("Default"))

        val JsFun = "JsFun".id()

        val JsOutlinedFunction = "JsOutlinedFunction".jsId()

        val JsGenerator = "JsGenerator".jsId()

        val DoNotIntrinsify = "DoNotIntrinsify".jsId()

        val annotationsRequiringExternal = setOf(JsModule, JsQualifier)

        val nativeAnnotations = setOf(JsNative, JsNativeInvoke, JsNativeGetter, JsNativeSetter)

        val JsNoLifting = "JsNoLifting".jsId()
    }

    @file:JvmField
    object Callables {
        val JsCode = "js".callableId(BASE_JS_PACKAGE)

        val JsDefinedExternally = "definedExternally".callableId(BASE_JS_PACKAGE)
    }
}

private fun String.jsId() = ClassId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
private fun String.id() = ClassId(BASE_KOTLIN_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
