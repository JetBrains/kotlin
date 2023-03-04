/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.name.Name

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irValueParameter(name: Name,
        block: IrElementBuilderClosure<IrValueParameterBuilder>) {
    declarationBuilders.add(IrValueParameterBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irValueParameter(name: String,
        block: IrElementBuilderClosure<IrValueParameterBuilder>) =
        irValueParameter(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irClass(name: Name,
        block: IrElementBuilderClosure<IrClassBuilder>) {
    declarationBuilders.add(IrClassBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irClass(name: String,
        block: IrElementBuilderClosure<IrClassBuilder>) =
        irClass(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irAnonymousInitializer(block: IrElementBuilderClosure<IrAnonymousInitializerBuilder>) {
    declarationBuilders.add(IrAnonymousInitializerBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeParameter(name: Name,
        block: IrElementBuilderClosure<IrTypeParameterBuilder>) {
    declarationBuilders.add(IrTypeParameterBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeParameter(name: String,
        block: IrElementBuilderClosure<IrTypeParameterBuilder>) =
        irTypeParameter(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irConstructor(name: Name,
        block: IrElementBuilderClosure<IrConstructorBuilder>) {
    declarationBuilders.add(IrConstructorBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irConstructor(name: String,
        block: IrElementBuilderClosure<IrConstructorBuilder>) =
        irConstructor(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irEnumEntry(name: Name,
        block: IrElementBuilderClosure<IrEnumEntryBuilder>) {
    declarationBuilders.add(IrEnumEntryBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irEnumEntry(name: String,
        block: IrElementBuilderClosure<IrEnumEntryBuilder>) =
        irEnumEntry(Name.guessByFirstCharacter(name), block)

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
inline fun IrDeclarationContainerBuilder.irField(name: Name,
        block: IrElementBuilderClosure<IrFieldBuilder>) {
    declarationBuilders.add(IrFieldBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irField(name: String,
        block: IrElementBuilderClosure<IrFieldBuilder>) =
        irField(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irLocalDelegatedProperty(name: Name,
        block: IrElementBuilderClosure<IrLocalDelegatedPropertyBuilder>) {
    declarationBuilders.add(IrLocalDelegatedPropertyBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irLocalDelegatedProperty(name: String,
        block: IrElementBuilderClosure<IrLocalDelegatedPropertyBuilder>) =
        irLocalDelegatedProperty(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irProperty(name: Name,
        block: IrElementBuilderClosure<IrPropertyBuilder>) {
    declarationBuilders.add(IrPropertyBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irProperty(name: String,
        block: IrElementBuilderClosure<IrPropertyBuilder>) =
        irProperty(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irScript(name: Name,
        block: IrElementBuilderClosure<IrScriptBuilder>) {
    declarationBuilders.add(IrScriptBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irScript(name: String,
        block: IrElementBuilderClosure<IrScriptBuilder>) =
        irScript(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irSimpleFunction(name: Name,
        block: IrElementBuilderClosure<IrSimpleFunctionBuilder>) {
    declarationBuilders.add(IrSimpleFunctionBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irSimpleFunction(name: String,
        block: IrElementBuilderClosure<IrSimpleFunctionBuilder>) =
        irSimpleFunction(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeAlias(name: Name,
        block: IrElementBuilderClosure<IrTypeAliasBuilder>) {
    declarationBuilders.add(IrTypeAliasBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeAlias(name: String,
        block: IrElementBuilderClosure<IrTypeAliasBuilder>) =
        irTypeAlias(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irVariable(name: Name,
        block: IrElementBuilderClosure<IrVariableBuilder>) {
    declarationBuilders.add(IrVariableBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irVariable(name: String,
        block: IrElementBuilderClosure<IrVariableBuilder>) =
        irVariable(Name.guessByFirstCharacter(name), block)
