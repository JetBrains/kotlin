/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.name.StandardClassIds.BASE_KOTLIN_PACKAGE

object WebCommonStandardClassIds {
    val BASE_JS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("js"))

    object Annotations {
        @JvmField
        val JsQualifier = "JsQualifier".jsId()

        @JvmField
        val JsModule = "JsModule".jsId()

        @JvmField
        val JsName = "JsName".jsId()

        @JvmField
        val JsExport = "JsExport".jsId()
    }

    object Callables {
        @JvmField
        val JsDefinedExternally = "definedExternally".callableId(BASE_JS_PACKAGE)

        @JvmField
        val Js = "js".callableId(BASE_JS_PACKAGE)
    }
}

private fun String.jsId() = ClassId(WebCommonStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
