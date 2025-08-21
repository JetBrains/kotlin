/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

private var IrElement._localClassType: Type? by irAttribute(copyByDefault = true)
// This sort of a hack for IR inliner case, where IrFunctionExpression stores and "shares" localClassType
// between IrFunctionReferences that are lowered from this IrFunctionExpression, but linked to it via attributeOwnerId.
// It can be changed to simply store the attribute on the function reference itself when either:
// - IR inliner (-Xserialize-ir) is dropped, or
// - IrFunctionExpression is dropped (KT-74383).
var IrElement.localClassType: Type?
    get() = attributeOwnerId._localClassType
    set(value) {
        attributeOwnerId._localClassType = value
    }

var IrFunction.enclosingMethodOverride: IrFunction? by irAttribute(copyByDefault = false)

var IrClass.localDelegatedProperties: List<IrLocalDelegatedPropertySymbol>? by irAttribute(copyByDefault = true)

var IrFunction.hasSpecialBridge: Boolean by irFlag(copyByDefault = false)

var IrSimpleFunction.overridesWithoutStubs: List<IrSimpleFunctionSymbol>? by irAttribute(copyByDefault = false)

var IrClass.multifileFacadeForPart: JvmClassName? by irAttribute(copyByDefault = true)
var IrClass.multifileFacadeClassForPart: IrClass? by irAttribute(copyByDefault = true)
var IrSimpleFunction.multifileFacadePartMember: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrConstructor.hiddenConstructorMangledParams: IrConstructor? by irAttribute(copyByDefault = false)
var IrConstructor.hiddenConstructorOfSealedClass: IrConstructor? by irAttribute(copyByDefault = false)

var IrClass.continuationClassVarsCountByType: Map<Type, Int>? by irAttribute(copyByDefault = true)

var IrClass.isPublicAbi: Boolean by irFlag(copyByDefault = false)

// If the JVM fqname of a class differs from what is implied by its parent, e.g. if it's a file class
// annotated with @JvmPackageName, the correct name is recorded here.
var IrClass.classNameOverride: JvmClassName? by irAttribute(copyByDefault = false)

var IrFunction.viewOfOriginalSuspendFunction: IrSimpleFunction? by irAttribute(copyByDefault = false)
var IrFunction.originalOfSuspendForInline: IrSimpleFunction? by irAttribute(copyByDefault = false)
var IrFunction.staticSuspendImplMethod: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrSimpleFunction.staticDefaultStub: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrElement.isEnclosedInConstructor: Boolean by irFlag(copyByDefault = true)

var IrVariable.originalSnippetValueSymbol: IrSymbol? by irAttribute(copyByDefault = false)

// SpecialAccessLowering replaces inaccessible calls in code fragment with reflective ones
// For Method.invoke(..) The original call is stored in this attribute
// It's used in suspend calls generation
var IrCall.originalForReflectiveCall: IrCall? by irAttribute(copyByDefault = false)