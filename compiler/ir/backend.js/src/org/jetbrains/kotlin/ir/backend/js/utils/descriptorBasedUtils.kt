/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.symbols.*

val IrFunctionSymbol.kind get() = descriptor.kind

val IrFunctionSymbol.modality get() = descriptor.modality

val IrConstructorSymbol.isPrimary get() = descriptor.isPrimary

val IrConstructorSymbol.constructedClassName get() = descriptor.constructedClass.name

val IrConstructorSymbol.constructedClass get() = descriptor.constructedClass

val IrValueSymbol.isSpecial get() = descriptor.name.isSpecial

val IrSymbol.name get() = descriptor.name

val IrClassSymbol.kind get() = descriptor.kind

val IrClassSymbol.modality get() = descriptor.modality

val IrClassSymbol.isAny get() = KotlinBuiltIns.isAny(descriptor)