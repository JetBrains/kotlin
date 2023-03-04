/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

@IrNodeBuilderDsl
inline fun buildIrFile(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrFileBuilder>
): IrFile = IrFileBuilder(name, buildingContext).apply(block).build()

@IrNodeBuilderDsl
inline fun buildIrModuleFragment(
    moduleDescriptor: ModuleDescriptor,
    irBuiltIns: IrBuiltIns,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrModuleFragmentBuilder>
): IrModuleFragment = IrModuleFragmentBuilder(moduleDescriptor, irBuiltIns, buildingContext).apply(block).build()

@IrNodeBuilderDsl
inline fun buildIrConstructor(
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrConstructorBuilder>,
): IrConstructor = buildIrConstructor(SpecialNames.INIT, buildingContext, block)
