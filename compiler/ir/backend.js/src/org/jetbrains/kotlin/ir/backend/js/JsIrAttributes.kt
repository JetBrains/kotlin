/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.backend.js.utils.findDefaultConstructorForReflection
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag

/**
 * Whether the class's ES6 constructor requires an additional `box` value parameter.
 */
internal var IrClass.needsBoxParameter: Boolean by irFlag(followAttributeOwner = false)

/**
 * The parameterless constructor that will be used to create instances with `KClass<*>.createInstance`.
 *
 * Note: for getting the actual constructor **do not** use this property, prefer [findDefaultConstructorForReflection], because
 * a constructor could be replaced by a factory function.
 * [findDefaultConstructorForReflection] will respect that.
 */
internal var IrClass.defaultConstructorForReflection: IrConstructor? by irAttribute(followAttributeOwner = false)

/**
 * The factory function which this constructor is replaced by.
 */
internal var IrConstructor.constructorFactory: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

/**
 * If [this] is a `main` function, the wrapper that actually calls the `main` function.
 *
 * @see org.jetbrains.kotlin.ir.backend.js.lower.MainFunctionCallWrapperLowering
 */
internal var IrSimpleFunction.mainFunctionWrapper: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

/**
 * If `this` is an object, contains the corresponding `getInstance` function that returns the single instance of the object.
 *
 * @see org.jetbrains.kotlin.ir.backend.js.lower.ObjectDeclarationLowering
 */
var IrClass.objectGetInstanceFunction: IrSimpleFunction? by irAttribute(followAttributeOwner = false)

/**
 * If `this` is an object, contains the field in which the singletone instance of the object is stored.
 *
 * @see org.jetbrains.kotlin.ir.backend.js.lower.ObjectDeclarationLowering
 */
internal var IrClass.objectInstanceField: IrField? by irAttribute(followAttributeOwner = false)
