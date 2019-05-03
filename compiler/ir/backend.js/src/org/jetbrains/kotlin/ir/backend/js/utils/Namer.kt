/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.js.backend.ast.JsNameRef

object Namer {
    val CALL_FUNCTION = "call"
    val BIND_FUNCTION = "bind"

    val SLICE_FUNCTION = "slice"

    val OUTER_NAME = "\$outer"
    val UNREACHABLE_NAME = "\$unreachable"

    val DELEGATE = "\$delegate"

    val IMPLICIT_RECEIVER_NAME = "this"

    val PROTOTYPE_NAME = "prototype"
    val CONSTRUCTOR_NAME = "constructor"

    val CONTINUATION = "\$cont"

    val JS_ERROR = JsNameRef("Error")

    val JS_OBJECT = JsNameRef("Object")
    val JS_OBJECT_CREATE_FUNCTION = JsNameRef("create", JS_OBJECT)

    val METADATA = "\$metadata\$"
    val METADATA_INTERFACES = "interfaces"
    val METADATA_SIMPLE_NAME = "simpleName"
    val METADATA_CLASS_KIND = "kind"

    val KCALLABLE_GET_NAME = "<get-name>"
    val KCALLABLE_NAME = "callableName"
    val KPROPERTY_GET = "get"
    val KPROPERTY_SET = "set"
    val KCALLABLE_CACHE_SUFFIX = "\$cache"
}