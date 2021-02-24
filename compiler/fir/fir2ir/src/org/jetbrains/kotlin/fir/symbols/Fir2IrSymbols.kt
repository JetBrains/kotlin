/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class Fir2IrClassSymbol(signature: IdSignature) :
    Fir2IrBindableSymbol<ClassDescriptor, IrClass>(signature),
    IrClassSymbol

class Fir2IrTypeAliasSymbol(signature: IdSignature) :
    Fir2IrBindableSymbol<TypeAliasDescriptor, IrTypeAlias>(signature),
    IrTypeAliasSymbol

class Fir2IrEnumEntrySymbol(signature: IdSignature) :
    Fir2IrBindableSymbol<ClassDescriptor, IrEnumEntry>(signature),
    IrEnumEntrySymbol

class Fir2IrSimpleFunctionSymbol(
    signature: IdSignature,
    containerSource: DeserializedContainerSource? = null
) : Fir2IrBindableSymbol<FunctionDescriptor, IrSimpleFunction>(signature, containerSource),
    IrSimpleFunctionSymbol

class Fir2IrConstructorSymbol(signature: IdSignature) :
    Fir2IrBindableSymbol<ClassConstructorDescriptor, IrConstructor>(signature),
    IrConstructorSymbol

class Fir2IrPropertySymbol(
    signature: IdSignature,
    containerSource: DeserializedContainerSource? = null
) : Fir2IrBindableSymbol<PropertyDescriptor, IrProperty>(signature, containerSource),
    IrPropertySymbol

