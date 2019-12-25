/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.ir.declarations.*

class JsMapping : DefaultMapping() {
    val singletonFieldDescriptors = newMapping<IrClass, IrField>()
    val outerThisFieldSymbols = newMapping<IrClass, IrField>()
    val innerClassConstructors = newMapping<IrConstructor, IrConstructor>()
    val originalInnerClassPrimaryConstructorByClass = newMapping<IrClass, IrConstructor>()
    val secondaryConstructorToDelegate = newMapping<IrConstructor, IrSimpleFunction>()
    val secondaryConstructorToFactory = newMapping<IrConstructor, IrSimpleFunction>()
    val objectToGetInstanceFunction = newMapping<IrClass, IrSimpleFunction>()
    val objectToInstanceField = newMapping<IrClass, IrField>()
    val classToSyntheticPrimaryConstructor = newMapping<IrClass, IrConstructor>()
}