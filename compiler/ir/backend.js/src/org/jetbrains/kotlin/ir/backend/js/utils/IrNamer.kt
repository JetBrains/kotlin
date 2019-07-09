/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsNameRef

interface IrNamer {
    fun getNameForConstructor(constructor: IrConstructor): JsName
    fun getNameForMemberFunction(function: IrSimpleFunction): JsName
    fun getNameForMemberField(field: IrField): JsName
    fun getNameForField(field: IrField): JsName
    fun getNameForValueDeclaration(declaration: IrValueDeclaration): JsName
    fun getNameForClass(klass: IrClass): JsName
    fun getNameForStaticFunction(function: IrSimpleFunction): JsName
    fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName
    fun getNameForProperty(property: IrProperty): JsName
    fun getRefForExternalClass(klass: IrClass): JsNameRef
    fun getNameForLoop(loop: IrLoop): JsName?
}