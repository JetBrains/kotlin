/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.v2.irClass
import org.jetbrains.kotlin.ir.builders.declarations.v2.irFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JvmReflectSymbols(val context: JvmBackendContext, irModule: IrModuleFragment) {
    private val irBuiltIns: IrBuiltIns = context.irBuiltIns

    private val javaLangReflect: FqName = FqName("java.lang.reflect")

    private val javaLangReflectPackage: IrPackageFragment =
        createEmptyExternalPackageFragment(irModule, javaLangReflect)

    val javaLangReflectField: IrClassSymbol =
        createJavaLangReflectClass(FqName("java.lang.reflect.Field")) { klass ->
            klass.irFunction {
                name = Name.identifier("setAccessible")
                returnType = irBuiltIns.unitType
                valueParameter("isAccessible", irBuiltIns.booleanType)
            }
            klass.irFunction {
                name = Name.identifier("get")
                returnType = irBuiltIns.anyNType
                valueParameter("receiver", irBuiltIns.anyNType)
            }
            klass.irFunction {
                name = Name.identifier("set")
                returnType = irBuiltIns.unitType
                valueParameter("receiver", irBuiltIns.anyNType)
                valueParameter("value", irBuiltIns.anyNType)
            }
        }

    val javaLangReflectMethod: IrClassSymbol =
        createJavaLangReflectClass(FqName("java.lang.reflect.Method")) { klass ->
            klass.irFunction {
                name = Name.identifier("setAccessible")
                returnType = irBuiltIns.unitType
                valueParameter("isAccessible", irBuiltIns.booleanType)
            }
            klass.irFunction {
                name = Name.identifier("invoke")
                returnType = irBuiltIns.anyNType
                valueParameter("receiver", irBuiltIns.anyNType)
                valueParameter {
                    name = Name.identifier("args")
                    type = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
                    varargElementType = irBuiltIns.anyNType
                }
            }
        }

    val javaLangReflectConstructor: IrClassSymbol =
        createJavaLangReflectClass(FqName("java.lang.reflect.Constructor")) { klass ->
            klass.irFunction {
                name = Name.identifier("setAccessible")
                returnType = irBuiltIns.unitType
                valueParameter("isAccessible", irBuiltIns.booleanType)
            }
            klass.irFunction {
                name = Name.identifier("newInstance")
                returnType = irBuiltIns.anyNType
                valueParameter {
                    name = Name.identifier("args")
                    type = irBuiltIns.arrayClass.typeWith(irBuiltIns.anyNType)
                    varargElementType = irBuiltIns.anyNType
                }
            }
        }

    init {
        val klass = context.symbols.javaLangClass.owner
        klass.irFunction {
            name = Name.identifier("getDeclaredMethod")
            returnType = javaLangReflectMethod.defaultType.makeNullable()
            valueParameter("methodName", irBuiltIns.stringType.makeNullable())
            valueParameter {
                name = Name.identifier("args")
                type = irBuiltIns.arrayClass.typeWith(klass.defaultType).makeNullable()
                varargElementType = klass.defaultType
            }
        }
        klass.irFunction {
            name = Name.identifier("getDeclaredField")
            returnType = javaLangReflectField.defaultType
            valueParameter("fieldName", irBuiltIns.stringType)
        }
        klass.irFunction {
            name = Name.identifier("getDeclaredConstructor")
            returnType = javaLangReflectConstructor.defaultType.makeNullable()
            valueParameter {
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
        context.symbols.javaLangClass.functionByName("getDeclaredField")

    val getDeclaredMethod: IrSimpleFunctionSymbol =
        context.symbols.javaLangClass.functionByName("getDeclaredMethod")

    val getDeclaredConstructor: IrSimpleFunctionSymbol =
        context.symbols.javaLangClass.functionByName("getDeclaredConstructor")

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
        block: (IrClass) -> Unit = {}
    ): IrClassSymbol =
        context.irFactory.irClass {
            name = fqName.shortName()
            kind = classKind
            modality = classModality
            parent = javaLangReflectPackage
            thisReceiver()
        }.apply {
            block(this)
        }.symbol
}
