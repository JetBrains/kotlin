/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.ir.declarations.*

class JsMapping : Mapping() {
    val outerThisFieldSymbols: DeclarationMapping<IrClass, IrField> by AttributeBasedMappingDelegate()
    val innerClassConstructors: DeclarationMapping<IrConstructor, IrConstructor> by AttributeBasedMappingDelegate()
    val originalInnerClassPrimaryConstructorByClass: DeclarationMapping<IrClass, IrConstructor> by AttributeBasedMappingDelegate()
    val objectToGetInstanceFunction: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val objectToInstanceField: DeclarationMapping<IrClass, IrField> by AttributeBasedMappingDelegate()
    val classToSyntheticPrimaryConstructor: DeclarationMapping<IrClass, IrConstructor> by AttributeBasedMappingDelegate()

    val enumEntryToGetInstanceFun: DeclarationMapping<IrEnumEntry, IrSimpleFunction> by AttributeBasedMappingDelegate()
    val enumEntryToInstanceField: DeclarationMapping<IrEnumEntry, IrField> by AttributeBasedMappingDelegate()
    val enumConstructorToNewConstructor: DeclarationMapping<IrConstructor, IrConstructor> by AttributeBasedMappingDelegate()
    val enumClassToCorrespondingEnumEntry: DeclarationMapping<IrClass, IrEnumEntry> by AttributeBasedMappingDelegate()
    val enumConstructorOldToNewValueParameters: DeclarationMapping<IrValueDeclaration, IrValueParameter> by AttributeBasedMappingDelegate()
    val enumEntryToCorrespondingField: DeclarationMapping<IrEnumEntry, IrField> by AttributeBasedMappingDelegate()
    val fieldToEnumEntry: DeclarationMapping<IrField, IrEnumEntry> by AttributeBasedMappingDelegate()
    val enumClassToInitEntryInstancesFun: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()

    val suspendArityStore: DeclarationMapping<IrClass, Collection<IrSimpleFunction>> by AttributeBasedMappingDelegate()

    val objectsWithPureInitialization: DeclarationMapping<IrClass, Boolean> by AttributeBasedMappingDelegate()

    // Wasm mappings
    val wasmNestedExternalToNewTopLevelFunction: DeclarationMapping<IrFunction, IrSimpleFunction> by AttributeBasedMappingDelegate()

    val wasmExternalObjectToGetInstanceFunction: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()

    val wasmExternalClassToInstanceCheck: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()

    val wasmGetJsClass: DeclarationMapping<IrClass, IrSimpleFunction> by AttributeBasedMappingDelegate()
}
