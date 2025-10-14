/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.backend.js.utils.findDefaultConstructorForReflection
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.backend.js.utils.findDefaultConstructorForReflection
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall

var IrClass.localClassName: String? by irAttribute(copyByDefault = true)

/**
 * Whether the class's ES6 constructor requires an additional `box` value parameter.
 */
internal var IrClass.needsBoxParameter: Boolean by irFlag(copyByDefault = false)

/**
 * The parameterless constructor that will be used to create instances with `KClass<*>.createInstance`.
 *
 * Note: for getting the actual constructor **do not** use this property, prefer [findDefaultConstructorForReflection], because
 * a constructor could be replaced by a factory function.
 * [findDefaultConstructorForReflection] will respect that.
 */
internal var IrClass.defaultConstructorForReflection: IrConstructor? by irAttribute(copyByDefault = false)

/**
 * The factory function which this constructor is replaced by.
 */
internal var IrConstructor.constructorFactory: IrSimpleFunction? by irAttribute(copyByDefault = false)

/**
 * If [this] is a `main` function, the wrapper that actually calls the `main` function.
 *
 * @see org.jetbrains.kotlin.ir.backend.js.lower.MainFunctionCallWrapperLowering
 */
internal var IrSimpleFunction.mainFunctionWrapper: IrSimpleFunction? by irAttribute(copyByDefault = false)

/**
 * If `this` is an object, contains the corresponding `getInstance` function that returns the single instance of the object.
 *
 * @see org.jetbrains.kotlin.ir.backend.js.lower.ObjectDeclarationLowering
 */
var IrClass.objectGetInstanceFunction: IrSimpleFunction? by irAttribute(copyByDefault = false)

/**
 * If `this` is an object, contains the field in which the singletone instance of the object is stored.
 *
 * @see org.jetbrains.kotlin.ir.backend.js.lower.ObjectDeclarationLowering
 */
internal var IrClass.objectInstanceField: IrField? by irAttribute(copyByDefault = false)

/**
 * The external constructor, whose delegation call was replaced in ES6 mode.
 * The attribute is used inside the [ES6PrimaryConstructorOptimizationLowering] to re-construct the original delegation call
 * in constructors that could be translated into simple ES6 class constructors.
 */
internal var IrCall.originalConstructor: IrConstructor? by irAttribute(copyByDefault = false)

var IrClass.syntheticPrimaryConstructor: IrConstructor? by irAttribute(copyByDefault = false)

var IrEnumEntry.getInstanceFun: IrSimpleFunction? by irAttribute(copyByDefault = false)
var IrEnumEntry.instanceField: IrField? by irAttribute(copyByDefault = false)
var IrConstructor.newEnumConstructor: IrConstructor? by irAttribute(copyByDefault = false)
var IrClass.correspondingEnumEntry: IrEnumEntry? by irAttribute(copyByDefault = false)
var IrValueDeclaration.valueParameterForOldEnumConstructor: IrValueParameter? by irAttribute(copyByDefault = false)
var IrEnumEntry.correspondingField: IrField? by irAttribute(copyByDefault = false)
var IrField.correspondingEnumEntry: IrEnumEntry? by irAttribute(copyByDefault = false)

/**
 * If the object being lowered is nested inside an enum class, we want to also initialize the enum entries when initializing the object.
 */
var IrClass.initEntryInstancesFun: IrSimpleFunction? by irAttribute(copyByDefault = false)

var IrClass.hasPureInitialization: Boolean? by irAttribute(copyByDefault = false)

/**
 * If a variable was moved from its original declaration place during lowering phase, we mark such a variable with this flag.
 * It helps us to recognize such variables and move them back (if it's possible) to the original declaration place.
 * We perform it on the JS AST optimization phase in [org.jetbrains.kotlin.js.inline.clean.MoveTemporaryVariableDeclarationToAssignment]
 */
internal var IrVariable.wasMovedFromItsDeclarationPlace: Boolean by irFlag(copyByDefault = false)
