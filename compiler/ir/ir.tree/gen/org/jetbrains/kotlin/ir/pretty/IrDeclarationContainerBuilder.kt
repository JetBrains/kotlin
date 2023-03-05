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
    __internal_addDeclarationBuilder(IrValueParameterBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irValueParameter(name: String,
        block: IrElementBuilderClosure<IrValueParameterBuilder>) =
        irValueParameter(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irClass(name: Name,
        block: IrElementBuilderClosure<IrClassBuilder>) {
    __internal_addDeclarationBuilder(IrClassBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irClass(name: String,
        block: IrElementBuilderClosure<IrClassBuilder>) =
        irClass(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irAnonymousInitializer(block: IrElementBuilderClosure<IrAnonymousInitializerBuilder>) {
    __internal_addDeclarationBuilder(IrAnonymousInitializerBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeParameter(name: Name,
        block: IrElementBuilderClosure<IrTypeParameterBuilder>) {
    __internal_addDeclarationBuilder(IrTypeParameterBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeParameter(name: String,
        block: IrElementBuilderClosure<IrTypeParameterBuilder>) =
        irTypeParameter(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irConstructor(name: Name,
        block: IrElementBuilderClosure<IrConstructorBuilder>) {
    __internal_addDeclarationBuilder(IrConstructorBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irConstructor(name: String,
        block: IrElementBuilderClosure<IrConstructorBuilder>) =
        irConstructor(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irEnumEntry(name: Name,
        block: IrElementBuilderClosure<IrEnumEntryBuilder>) {
    __internal_addDeclarationBuilder(IrEnumEntryBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irEnumEntry(name: String,
        block: IrElementBuilderClosure<IrEnumEntryBuilder>) =
        irEnumEntry(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irErrorDeclaration(block: IrElementBuilderClosure<IrErrorDeclarationBuilder>) {
    __internal_addDeclarationBuilder(IrErrorDeclarationBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irFunctionWithLateBinding(block: IrElementBuilderClosure<IrFunctionWithLateBindingBuilder>) {
    __internal_addDeclarationBuilder(IrFunctionWithLateBindingBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrDeclarationContainerBuilder.irPropertyWithLateBinding(block: IrElementBuilderClosure<IrPropertyWithLateBindingBuilder>) {
    __internal_addDeclarationBuilder(IrPropertyWithLateBindingBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irField(name: Name,
        block: IrElementBuilderClosure<IrFieldBuilder>) {
    __internal_addDeclarationBuilder(IrFieldBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irField(name: String,
        block: IrElementBuilderClosure<IrFieldBuilder>) =
        irField(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irLocalDelegatedProperty(name: Name,
        block: IrElementBuilderClosure<IrLocalDelegatedPropertyBuilder>) {
    __internal_addDeclarationBuilder(IrLocalDelegatedPropertyBuilder(name,
            buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irLocalDelegatedProperty(name: String,
        block: IrElementBuilderClosure<IrLocalDelegatedPropertyBuilder>) =
        irLocalDelegatedProperty(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irProperty(name: Name,
        block: IrElementBuilderClosure<IrPropertyBuilder>) {
    __internal_addDeclarationBuilder(IrPropertyBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irProperty(name: String,
        block: IrElementBuilderClosure<IrPropertyBuilder>) =
        irProperty(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irScript(name: Name,
        block: IrElementBuilderClosure<IrScriptBuilder>) {
    __internal_addDeclarationBuilder(IrScriptBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irScript(name: String,
        block: IrElementBuilderClosure<IrScriptBuilder>) =
        irScript(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irSimpleFunction(name: Name,
        block: IrElementBuilderClosure<IrSimpleFunctionBuilder>) {
    __internal_addDeclarationBuilder(IrSimpleFunctionBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irSimpleFunction(name: String,
        block: IrElementBuilderClosure<IrSimpleFunctionBuilder>) =
        irSimpleFunction(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeAlias(name: Name,
        block: IrElementBuilderClosure<IrTypeAliasBuilder>) {
    __internal_addDeclarationBuilder(IrTypeAliasBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irTypeAlias(name: String,
        block: IrElementBuilderClosure<IrTypeAliasBuilder>) =
        irTypeAlias(Name.guessByFirstCharacter(name), block)

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irVariable(name: Name,
        block: IrElementBuilderClosure<IrVariableBuilder>) {
    __internal_addDeclarationBuilder(IrVariableBuilder(name, buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrDeclarationContainerBuilder.irVariable(name: String,
        block: IrElementBuilderClosure<IrVariableBuilder>) =
        irVariable(Name.guessByFirstCharacter(name), block)
