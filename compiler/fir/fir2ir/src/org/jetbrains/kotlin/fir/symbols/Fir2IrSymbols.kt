/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*

class Fir2IrClassSymbol :
    Fir2IrBindableSymbol<ClassDescriptor, IrClass>(),
    IrClassSymbol

class Fir2IrEnumEntrySymbol :
    Fir2IrBindableSymbol<ClassDescriptor, IrEnumEntry>(),
    IrEnumEntrySymbol

class Fir2IrFieldSymbol :
    Fir2IrBindableSymbol<PropertyDescriptor, IrField>(),
    IrFieldSymbol

class Fir2IrTypeParameterSymbol :
    Fir2IrBindableSymbol<TypeParameterDescriptor, IrTypeParameter>(),
    IrTypeParameterSymbol

class Fir2IrValueParameterSymbol :
    Fir2IrBindableSymbol<ParameterDescriptor, IrValueParameter>(),
    IrValueParameterSymbol

class Fir2IrVariableSymbol :
    Fir2IrBindableSymbol<VariableDescriptor, IrVariable>(),
    IrVariableSymbol

class Fir2IrSimpleFunctionSymbol :
    Fir2IrBindableSymbol<FunctionDescriptor, IrSimpleFunction>(),
    IrSimpleFunctionSymbol

class Fir2IrConstructorSymbol :
    Fir2IrBindableSymbol<ClassConstructorDescriptor, IrConstructor>(),
    IrConstructorSymbol

class Fir2IrPropertySymbol :
    Fir2IrBindableSymbol<PropertyDescriptor, IrProperty>(),
    IrPropertySymbol

