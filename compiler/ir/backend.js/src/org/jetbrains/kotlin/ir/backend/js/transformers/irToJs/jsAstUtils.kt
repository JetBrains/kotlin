/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.js.backend.ast.JsDynamicScope
import org.jetbrains.kotlin.js.backend.ast.JsVars
import org.jetbrains.kotlin.name.Name

// TODO don't use JsDynamicScope
val dummyScope = JsDynamicScope

fun Name.toJsName() =
    // TODO sanitize
    dummyScope.declareName(asString())

fun jsVar(name: Name, initializer: IrExpression?): JsVars {
    val jsInitializer = initializer?.accept(IrElementToJsExpressionTransformer(), null)
    return JsVars(JsVars.JsVar(name.toJsName(), jsInitializer))
}