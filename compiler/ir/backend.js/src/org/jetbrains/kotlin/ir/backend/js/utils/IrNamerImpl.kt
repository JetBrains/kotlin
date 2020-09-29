/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.js.backend.ast.JsName

class IrNamerImpl(
    private val newNameTables: NameTables,
    private val context: JsIrBackendContext,
) : IrNamerBase() {
    override fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName =
        newNameTables.getNameForStaticDeclaration(declaration).toJsName()

    override fun getNameForMemberFunction(function: IrSimpleFunction): JsName {
        require(function.dispatchReceiverParameter != null)
        val signature = jsFunctionSignature(function, context)
        return signature.toJsName()
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
        if (property.isTopLevel) getNameForStaticDeclaration(property) else property.getJsNameOrKotlinName().asString().toJsName()

    override fun getRefForExternalClass(klass: IrClass): JsNameRef {
        val parent = klass.parent
        if (klass.isCompanion)
            return getRefForExternalClass(parent as IrClass)

        val currentClassName = if (klass.isTopLevel) getNameForStaticDeclaration(klass).ident else klass.getJsNameOrKotlinName().identifier
        return when (parent) {
            is IrClass ->
                JsNameRef(currentClassName, getRefForExternalClass(parent))

            is IrPackageFragment ->
                JsNameRef(currentClassName)

            else ->
                error("Unsupported external class parent $parent")
        }
    }

    private val associatedObjectKeyMap = mutableMapOf<IrClass, Int>()

    override fun getAssociatedObjectKey(irClass: IrClass): Int? {
        if (irClass.isAssociatedObjectAnnotatedAnnotation) {

            return associatedObjectKeyMap.getOrPut(irClass) { associatedObjectKeyMap.size }
        }
        return null
    }
}
