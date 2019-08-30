/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.backend.ast.JsRootScope

class IrNamerImpl(private val newNameTables: NameTables) : IrNamer {

    private fun String.toJsName() = JsName(this)

    override fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName =
        newNameTables.getNameForStaticDeclaration(declaration).toJsName()

    override fun getNameForLoop(loop: IrLoop): JsName? =
        newNameTables.getNameForLoop(loop)?.toJsName()

    override fun getNameForConstructor(constructor: IrConstructor): JsName {
        return getNameForStaticDeclaration(constructor.parentAsClass)
    }

    override fun getNameForMemberFunction(function: IrSimpleFunction): JsName {
        require(function.dispatchReceiverParameter != null)
        return newNameTables.getNameForMemberFunction(function).toJsName()
    }

    override fun getNameForMemberField(field: IrField): JsName {
        require(!field.isStatic)
        return newNameTables.getNameForMemberField(field).toJsName()
    }

    override fun getNameForField(field: IrField): JsName {
        return if (field.isStatic || field.parent is IrScript) {
            getNameForStaticDeclaration(field)
        } else {
            getNameForMemberField(field)
        }
    }

    override fun getNameForValueDeclaration(declaration: IrValueDeclaration): JsName =
        getNameForStaticDeclaration(declaration)

    override fun getNameForClass(klass: IrClass): JsName =
        getNameForStaticDeclaration(klass)

    override fun getNameForStaticFunction(function: IrSimpleFunction): JsName =
        getNameForStaticDeclaration(function)

    override fun getNameForProperty(property: IrProperty): JsName =
        property.getJsNameOrKotlinName().asString().toJsName()

    override fun getRefForExternalClass(klass: IrClass): JsNameRef {
        val parent = klass.parent
        if (klass.isCompanion)
            return getRefForExternalClass(parent as IrClass)

        val currentClassName = klass.getJsNameOrKotlinName().identifier
        return when (parent) {
            is IrClass ->
                JsNameRef(currentClassName, getRefForExternalClass(parent))

            is IrPackageFragment ->
                JsNameRef(currentClassName)

            else ->
                error("Unsupported external class parent $parent")
        }
    }
}
