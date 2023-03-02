/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.pretty


@PrettyIrDsl
interface IrDeclarationContainerBuilder {
    val declarationBuilders: MutableList<IrDeclarationBuilder<*>>

    val buildingContext: IrBuildingContext

    @IrNodeBuilderDsl
    fun irValueParameter(name: String,
            block: IrElementBuilderClosure<IrValueParameterBuilder>) {
        declarationBuilders.add(IrValueParameterBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irClass(name: String, block: IrElementBuilderClosure<IrClassBuilder>) {
        declarationBuilders.add(IrClassBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irAnonymousInitializer(block: IrElementBuilderClosure<IrAnonymousInitializerBuilder>) {
        declarationBuilders.add(IrAnonymousInitializerBuilder(buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irTypeParameter(name: String,
            block: IrElementBuilderClosure<IrTypeParameterBuilder>) {
        declarationBuilders.add(IrTypeParameterBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irConstructor(name: String, block: IrElementBuilderClosure<IrConstructorBuilder>) {
        declarationBuilders.add(IrConstructorBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irEnumEntry(name: String, block: IrElementBuilderClosure<IrEnumEntryBuilder>) {
        declarationBuilders.add(IrEnumEntryBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irErrorDeclaration(block: IrElementBuilderClosure<IrErrorDeclarationBuilder>) {
        declarationBuilders.add(IrErrorDeclarationBuilder(buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irFunctionWithLateBinding(block: IrElementBuilderClosure<IrFunctionWithLateBindingBuilder>) {
        declarationBuilders.add(IrFunctionWithLateBindingBuilder(buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irPropertyWithLateBinding(block: IrElementBuilderClosure<IrPropertyWithLateBindingBuilder>) {
        declarationBuilders.add(IrPropertyWithLateBindingBuilder(buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irField(name: String, block: IrElementBuilderClosure<IrFieldBuilder>) {
        declarationBuilders.add(IrFieldBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irLocalDelegatedProperty(name: String,
            block: IrElementBuilderClosure<IrLocalDelegatedPropertyBuilder>) {
        declarationBuilders.add(IrLocalDelegatedPropertyBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irModuleFragment(block: IrElementBuilderClosure<IrModuleFragmentBuilder>) {
        declarationBuilders.add(IrModuleFragmentBuilder(buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irProperty(name: String, block: IrElementBuilderClosure<IrPropertyBuilder>) {
        declarationBuilders.add(IrPropertyBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irScript(name: String, block: IrElementBuilderClosure<IrScriptBuilder>) {
        declarationBuilders.add(IrScriptBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irSimpleFunction(name: String,
            block: IrElementBuilderClosure<IrSimpleFunctionBuilder>) {
        declarationBuilders.add(IrSimpleFunctionBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irTypeAlias(name: String, block: IrElementBuilderClosure<IrTypeAliasBuilder>) {
        declarationBuilders.add(IrTypeAliasBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irVariable(name: String, block: IrElementBuilderClosure<IrVariableBuilder>) {
        declarationBuilders.add(IrVariableBuilder(name, buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irExternalPackageFragment(block: IrElementBuilderClosure<IrExternalPackageFragmentBuilder>) {
        declarationBuilders.add(IrExternalPackageFragmentBuilder(buildingContext).apply(block))
    }

    @IrNodeBuilderDsl
    fun irFile(block: IrElementBuilderClosure<IrFileBuilder>) {
        declarationBuilders.add(IrFileBuilder(buildingContext).apply(block))
    }
}
