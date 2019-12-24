/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


///
fun IrClassImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrClassSymbol,
    modality: Modality = symbol.descriptor.modality
) =
    IrClassImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name, symbol.descriptor.kind,
        symbol.descriptor.visibility,
        modality = modality,
        isCompanion = symbol.descriptor.isCompanionObject,
        isInner = symbol.descriptor.isInner,
        isData = symbol.descriptor.isData,
        isExternal = symbol.descriptor.isEffectivelyExternal(),
        isInline = symbol.descriptor.isInline,
        isExpect = symbol.descriptor.isExpect
    )


fun IrConstructorImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrConstructorSymbol,
    returnType: IrType,
    body: IrBody? = null
) =
    IrConstructorImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        symbol.descriptor.visibility,
        returnType,
        isInline = symbol.descriptor.isInline,
        isExternal = symbol.descriptor.isEffectivelyExternal(),
        isPrimary = symbol.descriptor.isPrimary,
        isExpect = symbol.descriptor.isExpect
    ).apply {
        this.body = body
    }


fun IrEnumEntryImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrEnumEntrySymbol
) =
    IrEnumEntryImpl(startOffset, endOffset, origin, symbol, symbol.descriptor.name)


///
fun IrExternalPackageFragmentImpl(symbol: IrExternalPackageFragmentSymbol) =
    IrExternalPackageFragmentImpl(symbol, symbol.descriptor.fqName)


fun IrFieldImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrFieldSymbol,
    type: IrType,
    visibility: Visibility = symbol.descriptor.visibility
) =
    IrFieldImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name, type, visibility,
        isFinal = !symbol.descriptor.isVar,
        isExternal = symbol.descriptor.isEffectivelyExternal(),
        isStatic = symbol.descriptor.dispatchReceiverParameter == null,
        isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
    )

fun IrFieldImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: PropertyDescriptor,
    type: IrType
): IrFieldImpl =
    IrFieldImpl(startOffset, endOffset, origin, IrFieldSymbolImpl(descriptor), type)


fun IrFileImpl(
    fileEntry: SourceManager.FileEntry,
    symbol: IrFileSymbol
) = IrFileImpl(fileEntry, symbol, symbol.descriptor.fqName)

fun IrFileImpl(
    fileEntry: SourceManager.FileEntry,
    packageFragmentDescriptor: PackageFragmentDescriptor
) = IrFileImpl(fileEntry, IrFileSymbolImpl(packageFragmentDescriptor), packageFragmentDescriptor.fqName)


fun IrFunctionImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrSimpleFunctionSymbol,
    returnType: IrType,
    visibility: Visibility = symbol.descriptor.visibility,
    modality: Modality = symbol.descriptor.modality
) =
    IrFunctionImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        visibility,
        modality,
        returnType,
        isInline = symbol.descriptor.isInline,
        isExternal = symbol.descriptor.isExternal,
        isTailrec = symbol.descriptor.isTailrec,
        isSuspend = symbol.descriptor.isSuspend,
        isExpect = symbol.descriptor.isExpect,
        isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
        isOperator = symbol.descriptor.isOperator
    )

// Used by kotlin-native in InteropLowering.kt and IrUtils2.kt
fun IrFunctionImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: FunctionDescriptor,
    returnType: IrType
): IrFunctionImpl =
    IrFunctionImpl(
        startOffset, endOffset, origin,
        IrSimpleFunctionSymbolImpl(descriptor), returnType
    )


fun IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrLocalDelegatedPropertySymbol,
    type: IrType
) =
    IrLocalDelegatedPropertyImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        type,
        symbol.descriptor.isVar
    )

@Deprecated("Creates unbound symbol", level = DeprecationLevel.ERROR)
fun IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptorWithAccessors,
    name: Name,
    type: IrType,
    isVar: Boolean
) =
    IrLocalDelegatedPropertyImpl(
        startOffset, endOffset, origin,
        IrLocalDelegatedPropertySymbolImpl(descriptor),
        name, type, isVar
    )

@Deprecated("Creates unbound symbol", level = DeprecationLevel.ERROR)
fun IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptorWithAccessors,
    type: IrType
) =
    IrLocalDelegatedPropertyImpl(
        startOffset, endOffset, origin,
        IrLocalDelegatedPropertySymbolImpl(descriptor),
        descriptor.name, type, descriptor.isVar
    )

@Suppress("DEPRECATION_ERROR")
@Deprecated("Creates unbound symbol", level = DeprecationLevel.ERROR)
fun IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptorWithAccessors,
    type: IrType,
    delegate: IrVariable,
    getter: IrFunction,
    setter: IrFunction?
): IrLocalDelegatedPropertyImpl =
    IrLocalDelegatedPropertyImpl(startOffset, endOffset, origin, descriptor, type).apply {
        this.delegate = delegate
        this.getter = getter
        this.setter = setter
    }


fun IrModuleFragmentImpl(descriptor: ModuleDescriptor, irBuiltins: IrBuiltIns, files: List<IrFile>) =
    IrModuleFragmentImpl(descriptor, irBuiltins).apply {
        this.files.addAll(files)
    }


@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: PropertyDescriptor,
    name: Name,
    visibility: Visibility,
    modality: Modality,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean
) =
    IrPropertyImpl(
        startOffset, endOffset, origin,
        IrPropertySymbolImpl(descriptor),
        name, visibility, modality,
        isVar = isVar,
        isConst = isConst,
        isLateinit = isLateinit,
        isDelegated = isDelegated,
        isExternal = isExternal
    )

@Suppress("DEPRECATION_ERROR")
@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    isDelegated: Boolean,
    descriptor: PropertyDescriptor
) =
    IrPropertyImpl(
        startOffset, endOffset, origin, descriptor,
        descriptor.name, descriptor.visibility, descriptor.modality,
        isVar = descriptor.isVar,
        isConst = descriptor.isConst,
        isLateinit = descriptor.isLateInit,
        isDelegated = isDelegated,
        isExternal = descriptor.isEffectivelyExternal()
    )

@Suppress("DEPRECATION", "DEPRECATION_ERROR")
@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: PropertyDescriptor
) =
    IrPropertyImpl(startOffset, endOffset, origin, descriptor.isDelegated, descriptor)

@Suppress("DEPRECATION_ERROR")
@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    isDelegated: Boolean,
    descriptor: PropertyDescriptor,
    backingField: IrField?
) =
    IrPropertyImpl(startOffset, endOffset, origin, isDelegated, descriptor).apply {
        this.backingField = backingField
    }

@Suppress("DEPRECATION_ERROR")
@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    isDelegated: Boolean,
    descriptor: PropertyDescriptor,
    backingField: IrField?,
    getter: IrSimpleFunction?,
    setter: IrSimpleFunction?
) =
    IrPropertyImpl(startOffset, endOffset, origin, isDelegated, descriptor, backingField).apply {
        this.getter = getter
        this.setter = setter
    }


fun IrTypeParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrTypeParameterSymbol
) =
    IrTypeParameterImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        symbol.descriptor.index,
        symbol.descriptor.isReified,
        symbol.descriptor.variance
    )

fun IrTypeParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrTypeParameterSymbol,
    name: Name,
    index: Int,
    variance: Variance
) =
    IrTypeParameterImpl(startOffset, endOffset, origin, symbol, name, index, symbol.descriptor.isReified, variance)

@Deprecated("Use constructor which takes symbol instead of descriptor", level = DeprecationLevel.ERROR)
fun IrTypeParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: TypeParameterDescriptor
) =
    IrTypeParameterImpl(startOffset, endOffset, origin, IrTypeParameterSymbolImpl(descriptor))


fun IrValueParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrValueParameterSymbol,
    type: IrType,
    varargElementType: IrType?
) =
    IrValueParameterImpl(
        startOffset, endOffset, origin,
        symbol,
        symbol.descriptor.name,
        symbol.descriptor.safeAs<ValueParameterDescriptor>()?.index ?: -1,
        type,
        varargElementType,
        symbol.descriptor.safeAs<ValueParameterDescriptor>()?.isCrossinline ?: false,
        symbol.descriptor.safeAs<ValueParameterDescriptor>()?.isNoinline ?: false
    )

fun IrValueParameterImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: ParameterDescriptor,
    type: IrType,
    varargElementType: IrType?
) =
    IrValueParameterImpl(startOffset, endOffset, origin, IrValueParameterSymbolImpl(descriptor), type, varargElementType)


fun IrVariableImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrVariableSymbol,
    type: IrType
) =
    IrVariableImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name, type,
        isVar = symbol.descriptor.isVar,
        isConst = symbol.descriptor.isConst,
        isLateinit = symbol.descriptor.isLateInit
    )

fun IrVariableImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptor,
    type: IrType
) =
    IrVariableImpl(startOffset, endOffset, origin, IrVariableSymbolImpl(descriptor), type)

fun IrVariableImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: VariableDescriptor,
    type: IrType,
    initializer: IrExpression?
) =
    IrVariableImpl(startOffset, endOffset, origin, descriptor, type).apply {
        this.initializer = initializer
    }
