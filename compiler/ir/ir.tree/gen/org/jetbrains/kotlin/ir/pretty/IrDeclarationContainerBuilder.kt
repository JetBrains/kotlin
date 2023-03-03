/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.pretty


@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irValueParameter(name: String,
        block: IrElementBuilderClosure<IrValueParameterBuilder>) {
    declarationBuilders.add(IrValueParameterBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irClass(name: String,
        block: IrElementBuilderClosure<IrClassBuilder>) {
    declarationBuilders.add(IrClassBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irAnonymousInitializer(block: IrElementBuilderClosure<IrAnonymousInitializerBuilder>) {
    declarationBuilders.add(IrAnonymousInitializerBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeParameter(name: String,
        block: IrElementBuilderClosure<IrTypeParameterBuilder>) {
    declarationBuilders.add(IrTypeParameterBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irConstructor(name: String,
        block: IrElementBuilderClosure<IrConstructorBuilder>) {
    declarationBuilders.add(IrConstructorBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irEnumEntry(name: String,
        block: IrElementBuilderClosure<IrEnumEntryBuilder>) {
    declarationBuilders.add(IrEnumEntryBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irErrorDeclaration(block: IrElementBuilderClosure<IrErrorDeclarationBuilder>) {
    declarationBuilders.add(IrErrorDeclarationBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irFunctionWithLateBinding(block: IrElementBuilderClosure<IrFunctionWithLateBindingBuilder>) {
    declarationBuilders.add(IrFunctionWithLateBindingBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irPropertyWithLateBinding(block: IrElementBuilderClosure<IrPropertyWithLateBindingBuilder>) {
    declarationBuilders.add(IrPropertyWithLateBindingBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irField(name: String,
        block: IrElementBuilderClosure<IrFieldBuilder>) {
    declarationBuilders.add(IrFieldBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irLocalDelegatedProperty(name: String,
        block: IrElementBuilderClosure<IrLocalDelegatedPropertyBuilder>) {
    declarationBuilders.add(IrLocalDelegatedPropertyBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irModuleFragment(block: IrElementBuilderClosure<IrModuleFragmentBuilder>) {
    declarationBuilders.add(IrModuleFragmentBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irProperty(name: String,
        block: IrElementBuilderClosure<IrPropertyBuilder>) {
    declarationBuilders.add(IrPropertyBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irScript(name: String,
        block: IrElementBuilderClosure<IrScriptBuilder>) {
    declarationBuilders.add(IrScriptBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irSimpleFunction(name: String,
        block: IrElementBuilderClosure<IrSimpleFunctionBuilder>) {
    declarationBuilders.add(IrSimpleFunctionBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeAlias(name: String,
        block: IrElementBuilderClosure<IrTypeAliasBuilder>) {
    declarationBuilders.add(IrTypeAliasBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irVariable(name: String,
        block: IrElementBuilderClosure<IrVariableBuilder>) {
    declarationBuilders.add(IrVariableBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irExternalPackageFragment(block: IrElementBuilderClosure<IrExternalPackageFragmentBuilder>) {
    declarationBuilders.add(IrExternalPackageFragmentBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irFile(block: IrElementBuilderClosure<IrFileBuilder>) {
    declarationBuilders.add(IrFileBuilder(buildingContext).apply(block))
}
