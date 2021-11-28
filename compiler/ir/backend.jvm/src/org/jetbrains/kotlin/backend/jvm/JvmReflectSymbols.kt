/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JvmReflectSymbols(val context: JvmBackendContext) {
    private val irBuiltIns: IrBuiltIns = context.irBuiltIns

    private val javaLangReflect: FqName = FqName("java.lang.reflect")

    private val javaLangReflectPackage: IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(context.state.module, javaLangReflect)

    val javaLangReflectField: IrClassSymbol =
        createJavaLangReflectClass(FqName("java.lang.reflect.Field")) { klass ->
            klass.addFunction("setAccessible", irBuiltIns.unitType).apply {
                addValueParameter("isAccessible", irBuiltIns.booleanType)
            }
            klass.addFunction("get", irBuiltIns.anyNType).apply {
                addValueParameter("receiver", irBuiltIns.anyNType)
            }
            klass.addFunction("set", irBuiltIns.unitType).apply {
                addValueParameter("receiver", irBuiltIns.anyNType)
                addValueParameter("value", irBuiltIns.anyNType)
            }
        }

    val javaLangReflectMethod: IrClassSymbol =
        createJavaLangReflectClass(FqName("java.lang.reflect.Method")) { klass ->
            klass.addFunction("setAccessible", irBuiltIns.unitType).apply {
                addValueParameter("isAccessible", irBuiltIns.booleanType)
            }
            klass.addFunction("invoke", irBuiltIns.anyNType).apply {
                addValueParameter("receiver", irBuiltIns.anyNType)
                addValueParameter {
                    name = Name.identifier("args")
                    type = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
                    varargElementType = irBuiltIns.anyNType
                }
            }
        }

    val javaLangReflectConstructor: IrClassSymbol =
        createJavaLangReflectClass(FqName("java.lang.reflect.Constructor")) { klass ->
            klass.addFunction("setAccessible", irBuiltIns.unitType).apply {
                addValueParameter("isAccessible", irBuiltIns.booleanType)
            }
            klass.addFunction("newInstance", irBuiltIns.anyNType).apply {
                addValueParameter {
                    name = Name.identifier("args")
                    type = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
                    varargElementType = irBuiltIns.anyNType
                }
            }
        }

    init {
        val klass = context.ir.symbols.javaLangClass.owner
        klass.addFunction("getDeclaredMethod", javaLangReflectMethod.defaultType.makeNullable()).apply {
            addValueParameter("methodName", irBuiltIns.stringType.makeNullable())
            addValueParameter {
                name = Name.identifier("args")
                type = irBuiltIns.arrayClass.typeWith(klass.defaultType).makeNullable()
                varargElementType = klass.defaultType
            }
        }
        klass.addFunction("getDeclaredField", javaLangReflectField.defaultType).apply {
            addValueParameter("fieldName", irBuiltIns.stringType)
        }
        klass.addFunction("getDeclaredConstructor", javaLangReflectConstructor.defaultType.makeNullable()).apply {
            addValueParameter {
                name = Name.identifier("args")
                type = irBuiltIns.arrayClass.typeWith(klass.defaultType).makeNullable()
                varargElementType = klass.defaultType
            }
        }
    }

    val javaLangReflectFieldSetAccessible: IrSimpleFunctionSymbol =
        javaLangReflectField.functionByName("setAccessible")

    val javaLangReflectMethodSetAccessible: IrSimpleFunctionSymbol =
        javaLangReflectMethod.functionByName("setAccessible")

    val javaLangReflectConstructorSetAccessible: IrSimpleFunctionSymbol =
        javaLangReflectConstructor.functionByName("setAccessible")

    val getDeclaredField: IrSimpleFunctionSymbol =
        context.ir.symbols.javaLangClass.functionByName("getDeclaredField")

    val getDeclaredMethod: IrSimpleFunctionSymbol =
        context.ir.symbols.javaLangClass.functionByName("getDeclaredMethod")

    val getDeclaredConstructor: IrSimpleFunctionSymbol =
        context.ir.symbols.javaLangClass.functionByName("getDeclaredConstructor")

    val javaLangReflectFieldGet: IrSimpleFunctionSymbol =
        javaLangReflectField.functionByName("get")

    val javaLangReflectFieldSet: IrSimpleFunctionSymbol =
        javaLangReflectField.functionByName("set")

    val javaLangReflectMethodInvoke: IrSimpleFunctionSymbol =
        javaLangReflectMethod.functionByName("invoke")

    val javaLangReflectConstructorNewInstance: IrSimpleFunctionSymbol =
        javaLangReflectConstructor.functionByName("newInstance")

    private fun createJavaLangReflectClass(
        fqName: FqName,
        classKind: ClassKind = ClassKind.CLASS,
        classModality: Modality = Modality.FINAL,
        classIsInline: Boolean = false,
        block: (IrClass) -> Unit = {}
    ): IrClassSymbol =
        context.irFactory.buildClass {
            name = fqName.shortName()
            kind = classKind
            modality = classModality
            isInline = classIsInline
        }.apply {
            parent = javaLangReflectPackage
            createImplicitParameterDeclarationWithWrappedDescriptor()
            block(this)
        }.symbol
}