/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.name

/**
 * Consult KDoc of [StandardClassIds] regarding naming rules when adding declarations here.
 * Also, check if the desired id is already present there.
 */
object WebCommonStandardClassIds {
    object Annotations {
        val JsQualifier = "JsQualifier".jsId()
        val JsModule = "JsModule".jsId()
        val JsName = "JsName".jsId()
        val JsExport = "JsExport".jsId()

        val native = "native".jsId()
        val nativeInvoke = "nativeInvoke".jsId()
    }

    object Callables {
        val js = "js".callableId(StandardClassIds.BASE_JS_PACKAGE)
        val definedExternally = "definedExternally".callableId(StandardClassIds.BASE_JS_PACKAGE)
    }
}

private fun String.jsId() = ClassId(StandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))

private fun String.callableId(packageName: FqName) = CallableId(packageName, Name.identifier(this))
