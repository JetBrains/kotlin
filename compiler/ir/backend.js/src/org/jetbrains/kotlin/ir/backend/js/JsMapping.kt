/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.DefaultDelegateFactory
import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.ir.backend.js.utils.MutableReference
import org.jetbrains.kotlin.ir.declarations.*

class JsMapping : DefaultMapping() {
    val esClassWhichNeedBoxParameters = DefaultDelegateFactory.newDeclarationToValueMapping<IrClass, Boolean>()
    val esClassToPossibilityForOptimization = DefaultDelegateFactory.newDeclarationToValueMapping<IrClass, MutableReference<Boolean>>()

    val outerThisFieldSymbols = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrField>()
    val innerClassConstructors = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrConstructor, IrConstructor>()
    val originalInnerClassPrimaryConstructorByClass = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrConstructor>()
    val secondaryConstructorToDelegate = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrConstructor, IrSimpleFunction>()
    val secondaryConstructorToFactory = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrConstructor, IrSimpleFunction>()
    val objectToGetInstanceFunction = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
    val objectToInstanceField = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrField>()
    val classToSyntheticPrimaryConstructor = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrConstructor>()
    val privateMemberToCorrespondingStatic = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrFunction, IrSimpleFunction>()

    val enumEntryToGetInstanceFun = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrEnumEntry, IrSimpleFunction>()
    val enumEntryToInstanceField = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrEnumEntry, IrField>()
    val enumConstructorToNewConstructor = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrConstructor, IrConstructor>()
    val enumClassToCorrespondingEnumEntry = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrEnumEntry>()
    val enumConstructorOldToNewValueParameters = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrValueDeclaration, IrValueParameter>()
    val enumEntryToCorrespondingField = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrEnumEntry, IrField>()
    val fieldToEnumEntry = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrField, IrEnumEntry>()
    val enumClassToInitEntryInstancesFun = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()

    val suspendArityStore = DefaultDelegateFactory.newDeclarationToDeclarationCollectionMapping<IrClass, Collection<IrSimpleFunction>>()

    val inlineFunctionsBeforeInlining = DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrFunction, IrFunction>()

    // Wasm mappings
    val wasmJsInteropFunctionToWrapper =
        DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrSimpleFunction, IrSimpleFunction>()

    val wasmNestedExternalToNewTopLevelFunction =
        DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrFunction, IrSimpleFunction>()

    val wasmExternalObjectToGetInstanceFunction =
        DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()

    val wasmExternalClassToInstanceCheck =
        DefaultDelegateFactory.newDeclarationToDeclarationMapping<IrClass, IrSimpleFunction>()
}
