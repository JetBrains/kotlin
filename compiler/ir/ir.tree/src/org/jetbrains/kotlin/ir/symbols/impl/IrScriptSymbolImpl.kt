/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.symbols.impl

import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol

class IrScriptSymbolImpl(descriptor: ScriptDescriptor) :
    IrScriptSymbol, IrBindableSymbolBase<ScriptDescriptor, IrScript>(descriptor)
