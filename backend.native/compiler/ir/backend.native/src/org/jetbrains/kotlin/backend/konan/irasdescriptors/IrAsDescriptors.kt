/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.irasdescriptors

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol

internal typealias DeclarationDescriptor = IrDeclaration
internal typealias FunctionDescriptor = IrFunction
internal typealias ClassDescriptor = IrClass
internal typealias ConstructorDescriptor = IrConstructor
internal typealias ClassConstructorDescriptor = IrConstructor
internal typealias PackageFragmentDescriptor = IrPackageFragment
internal typealias VariableDescriptor = IrVariable
internal typealias ValueDescriptor = IrValueDeclaration
internal typealias ParameterDescriptor = IrValueParameter
internal typealias ValueParameterDescriptor = IrValueParameter
internal typealias TypeParameterDescriptor = IrTypeParameter
