/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * Consult KDoc of [StandardClassIds] regarding naming rules when adding declarations here.
 * Also, check if the desired id is already present there.
 */
object JsStandardClassIds {
    val BASE_JS_INTERNAL_PACKAGE = StandardClassIds.BASE_JS_PACKAGE.child(Name.identifier("internal"))
    val BASE_JS_INTERNAL_BOXEDLONG_PACKAGE = BASE_JS_INTERNAL_PACKAGE.child(Name.identifier("boxedLong"))
    val BASE_JS_INTERNAL_LONGASBIGINT_PACKAGE = BASE_JS_INTERNAL_PACKAGE.child(Name.identifier("longAsBigInt"))
    val BASE_REFLECT_JS_INTERNAL_PACKAGE =
        StandardClassIds.BASE_REFLECT_PACKAGE.child(Name.identifier("js")).child(Name.identifier("internal"))

    val Promise = "Promise".jsId()
    val JsObject = "JsObject".jsId()
    val Date = "Date".jsId()
    val JsAny = "JsAny".jsId()
    val JsReference = "JsReference".jsId()

    object Annotations {
        val JsQualifier = "JsQualifier".jsId()
        val JsModule = "JsModule".jsId()
        val JsNonModule = "JsNonModule".jsId()

        val native = "native".jsId()
        val library = "library".jsId()
        val nativeInvoke = "nativeInvoke".jsId()
        val nativeGetter = "nativeGetter".jsId()
        val nativeSetter = "nativeSetter".jsId()

        val JsName = "JsName".jsId()
        val JsSymbol = "JsSymbol".jsId()

        val JsExport = "JsExport".jsId()
        val JsExport_Ignore = JsExport.createNestedClassId(Name.identifier("Ignore"))
        val JsExport_Default = JsExport.createNestedClassId(Name.identifier("Default"))
        val JsImplicitExport = "JsImplicitExport".jsId()
        val JsDontExportDefaultImplementation = "JsDontExportDefaultImplementation".jsId()

        val JsNoDispatchReceiver = "JsNoDispatchReceiver".jsId()
        val JsStatic = "JsStatic".jsId()
        val JsExternalInheritorsOnly = "JsExternalInheritorsOnly".jsId()
        val JsExternalArgument = "JsExternalArgument".jsId()
        val EagerInitialization = "EagerInitialization".jsId()
        val JsFun = "JsFun".baseId()
        val JsOutlinedFunction = "JsOutlinedFunction".jsId()
        val DoNotIntrinsify = "DoNotIntrinsify".jsId()
        val JsNoLifting = "JsNoLifting".jsId()
        val JsNoRuntime = "JsNoRuntime".jsId()
        val JsFileName = "JsFileName".jsId()

        val annotationsRequiringExternal = setOf(JsModule, JsQualifier)
        val nativeAnnotations = setOf(native, nativeInvoke, nativeGetter, nativeSetter)
    }

    object Callables {
        val js = "js".callableId(StandardClassIds.BASE_JS_PACKAGE)
        val definedExternally = "definedExternally".callableId(StandardClassIds.BASE_JS_PACKAGE)
    }
}

private fun String.baseId() = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier(this))
private fun String.jsId() = ClassId(StandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
