/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("IrDeclarationBuilders")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ir.builders.declarations.v2

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.copyAnnotations
import org.jetbrains.kotlin.ir.util.substitute

/**
 * What references to the type parameters and the parameters of a function are replaced with in a copy of its signature
 * (see [IrFunctionBuilder.copySignatureFrom]), e.g. to copy default values or to move the body.
 */
class IrSignatureMapping(
    val typeSubstitution: Map<IrTypeParameterSymbol, IrType>,
    val parameters: Map<IrValueParameterSymbol, IrValueParameterSymbol>,
)

/*
 * Copying type parameters and parameters depends on the order of initialization: all the copies of type parameters must exist
 * before their supertypes and the types of parameters can be substituted.
 */

internal fun buildTypeParameterCopies(
    builder: IrTypeParametersContainerBuilder,
    source: List<IrTypeParameter>,
    adjust: IrTypeParameterBuilder.(from: IrTypeParameter) -> Unit,
): Map<IrTypeParameterSymbol, IrType> {
    val copies = source.map { from ->
        IrTypeParameterBuilder().apply {
            updateFrom(from)
            annotations = from.copyAnnotations()
            adjust(from)
        }
    }
    builder.typeParameters += copies
    val substitution = source.zip(copies).associate { [from, copy] -> from.symbol to copy.symbol.defaultType }
    for ([from, copy] in source.zip(copies)) {
        copy.superTypes = from.superTypes.map { it.substitute(substitution) }
    }
    return substitution
}

internal fun buildParameterCopies(
    builder: IrFunctionBuilder,
    source: List<IrValueParameter>,
    substitution: Map<IrTypeParameterSymbol, IrType>,
    adjust: IrValueParameterBuilder.(from: IrValueParameter) -> Unit,
): Map<IrValueParameterSymbol, IrValueParameterSymbol> =
    source.associate { from ->
        from.symbol to builder.parameter {
            kind = from.kind
            updateFrom(from)
            type = from.type.substitute(substitution)
            varargElementType = from.varargElementType?.substitute(substitution)
            annotations = from.copyAnnotations()
            adjust(from)
        }
    }

internal fun buildSignatureCopy(builder: IrFunctionBuilder, source: IrFunction): IrSignatureMapping {
    val typeSubstitution = buildTypeParameterCopies(builder, source.typeParameters) {}
    val parameters = buildParameterCopies(builder, source.parameters, typeSubstitution) {}
    builder.returnType = source.returnType.substitute(typeSubstitution)
    return IrSignatureMapping(typeSubstitution, parameters)
}
