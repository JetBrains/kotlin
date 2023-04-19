/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsNameRef

interface IrNamer {
    fun getNameForMemberFunction(function: IrSimpleFunction): JsName
    fun getNameForMemberField(field: IrField): JsName
    fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName
    fun getNameForStaticFunction(function: IrSimpleFunction): JsName
    fun getNameForField(field: IrField): JsName
    fun getNameForConstructor(constructor: IrConstructor): JsName
    fun getNameForClass(klass: IrClass): JsName
    fun getRefForExternalClass(klass: IrClass): JsNameRef
    fun getNameForProperty(property: IrProperty): JsName
    fun getAssociatedObjectKey(irClass: IrClass): Int?
}

abstract class IrNamerBase : IrNamer {
    abstract override fun getNameForMemberFunction(function: IrSimpleFunction): JsName
    abstract override fun getNameForMemberField(field: IrField): JsName
    abstract override fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName

    protected fun String.toJsName(temporary: Boolean = true) = JsName(this, temporary)

    override fun getNameForStaticFunction(function: IrSimpleFunction): JsName =
        getNameForStaticDeclaration(function)

    override fun getNameForField(field: IrField): JsName {
        return if (field.isStatic || field.parent is IrScript) {
            getNameForStaticDeclaration(field)
        } else {
            getNameForMemberField(field)
        }
    }

    override fun getNameForConstructor(constructor: IrConstructor): JsName =
        getNameForStaticDeclaration(constructor.parentAsClass)

    override fun getNameForClass(klass: IrClass): JsName =
        getNameForStaticDeclaration(klass)

    override fun getRefForExternalClass(klass: IrClass): JsNameRef {
        val parent = klass.parent
        if (klass.isCompanion)
            return getRefForExternalClass(parent as IrClass)

        val currentClassName = klass.getJsNameOrKotlinName().identifier
        return when (parent) {
            is IrClass ->
                JsNameRef(currentClassName, getRefForExternalClass(parent))

            is IrPackageFragment -> {
                getNameForStaticDeclaration(klass).makeRef()
            }

            else ->
                error("Unsupported external class parent $parent")
        }
    }

    override fun getNameForProperty(property: IrProperty): JsName {
        return if (property.parent is IrClass) {
            property.getJsNameOrKotlinName().asString().toJsName()
        } else {
            getNameForStaticDeclaration(property)
        }
    }

    private val associatedObjectKeyMap = hashMapOf<IrClass, Int>()

    override fun getAssociatedObjectKey(irClass: IrClass): Int? {
        if (irClass.isAssociatedObjectAnnotatedAnnotation) {

            return associatedObjectKeyMap.getOrPut(irClass) { associatedObjectKeyMap.size }
        }
        return null
    }
}