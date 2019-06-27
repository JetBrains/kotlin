/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.org.objectweb.asm.Type

fun IrTypeMapper.mapType(irVariable: IrVariable): Type =
    mapType(irVariable.type)

fun IrTypeMapper.mapType(irValueParameter: IrValueParameter): Type =
    mapType(irValueParameter.type)

fun IrTypeMapper.mapType(irField: IrField): Type =
    mapType(irField.type)

fun IrTypeMapper.mapSupertype(irType: IrType, sw: JvmSignatureWriter): Type =
    mapType(irType, TypeMappingMode.SUPER_TYPE, sw)

fun IrTypeMapper.mapClass(irClass: IrClass): Type =
    mapType(irClass.defaultType, TypeMappingMode.CLASS_DECLARATION)

fun IrTypeMapper.mapTypeAsDeclaration(irType: IrType): Type =
    mapType(irType, TypeMappingMode.CLASS_DECLARATION)

fun IrTypeMapper.mapTypeParameter(irType: IrType, sw: JvmSignatureWriter): Type =
    mapType(irType, TypeMappingMode.GENERIC_ARGUMENT, sw)
