/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

var IrAttributeContainer.localClassType: Type? by irAttribute(followAttributeOwner = true)

var IrFunction.enclosingMethodOverride: IrFunction? by irAttribute(followAttributeOwner = false)

var IrClass.localDelegatedProperties: List<IrLocalDelegatedPropertySymbol>? by irAttribute(followAttributeOwner = true)

var IrFunction.hasSpecialBridge: Boolean by irFlag(followAttributeOwner = false)

var IrSimpleFunction.overridesWithoutStubs: List<IrSimpleFunctionSymbol>? by irAttribute(followAttributeOwner = false)

var IrClass.multifileFacadeForPart: JvmClassName? by irAttribute(followAttributeOwner = true)
var IrClass.multifileFacadeClassForPart: IrClass? by irAttribute(followAttributeOwner = true)
var IrSimpleFunction.multifileFacadePartMember: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

var IrConstructor.hiddenConstructorMangledParams: IrConstructor? by irAttribute(followAttributeOwner = false)
var IrConstructor.hiddenConstructorOfSealedClass: IrConstructor? by irAttribute(followAttributeOwner = false)

var IrClass.continuationClassVarsCountByType: Map<Type, Int>? by irAttribute(followAttributeOwner = true)