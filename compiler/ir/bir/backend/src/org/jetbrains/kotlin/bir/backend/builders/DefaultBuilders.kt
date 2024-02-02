/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.builders

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.expressions.impl.*
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.types.BirUninitializedType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirAnonymousInitializer.Companion.build(init: BirAnonymousInitializerImpl.() -> Unit): BirAnonymousInitializerImpl =
    BirAnonymousInitializerImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        origin = IrDeclarationOrigin.DEFINED,
        isStatic = false,
        body = BirBlockBodyImpl(SourceSpan.UNDEFINED),
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirConstructor.Companion.build(init: BirConstructorImpl.() -> Unit): BirConstructorImpl =
    BirConstructorImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = SpecialNames.INIT,
        origin = IrDeclarationOrigin.DEFINED,
        visibility = DescriptorVisibilities.PUBLIC,
        isExternal = false,
        isInline = false,
        isExpect = false,
        isPrimary = false,
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirClass.Companion.build(init: BirClassImpl.() -> Unit): BirClassImpl =
    BirClassImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = UninitializedName,
        origin = IrDeclarationOrigin.DEFINED,
        source = SourceElement.NO_SOURCE,
        kind = ClassKind.CLASS,
        visibility = DescriptorVisibilities.PUBLIC,
        modality = Modality.FINAL,
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirEnumEntry.Companion.build(init: BirEnumEntryImpl.() -> Unit): BirEnumEntryImpl =
    BirEnumEntryImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = UninitializedName,
        origin = IrDeclarationOrigin.DEFINED,
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirField.Companion.build(init: BirFieldImpl.() -> Unit): BirFieldImpl =
    BirFieldImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = UninitializedName,
        origin = IrDeclarationOrigin.DEFINED,
        visibility = DescriptorVisibilities.PUBLIC,
        isExternal = false,
        type = BirUninitializedType,
        isFinal = false,
        isStatic = false,
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirLocalDelegatedProperty.Companion.build(init: BirLocalDelegatedPropertyImpl.() -> Unit): BirLocalDelegatedPropertyImpl =
    BirLocalDelegatedPropertyImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = UninitializedName,
        origin = IrDeclarationOrigin.DEFINED,
        type = BirUninitializedType,
        isVar = false,
        delegate = BirVariable.build {},
        getter = BirSimpleFunction.build { },
        setter = null,
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirProperty.Companion.build(init: BirPropertyImpl.() -> Unit): BirPropertyImpl =
    BirPropertyImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = UninitializedName,
        origin = IrDeclarationOrigin.DEFINED,
        visibility = DescriptorVisibilities.PUBLIC,
        modality = Modality.FINAL,
        isExternal = false,
        isExpect = false,
        isFakeOverride = false,
        isVar = false,
        isConst = false,
        isLateinit = false,
        isDelegated = false,
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirSimpleFunction.Companion.build(init: BirSimpleFunctionImpl.() -> Unit): BirSimpleFunctionImpl =
    BirSimpleFunctionImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = UninitializedName,
        origin = IrDeclarationOrigin.DEFINED,
        visibility = DescriptorVisibilities.PUBLIC,
        modality = Modality.FINAL,
        isExternal = false,
        isInline = false,
        isExpect = false,
        isFakeOverride = false,
        isTailrec = false,
        isSuspend = false,
        isOperator = false,
        isInfix = false,
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirTypeParameter.Companion.build(init: BirTypeParameterImpl.() -> Unit): BirTypeParameterImpl =
    BirTypeParameterImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = UninitializedName,
        origin = IrDeclarationOrigin.DEFINED,
        variance = Variance.INVARIANT,
        isReified = false,
        index = 0,
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirValueParameter.Companion.build(init: BirValueParameterImpl.() -> Unit): BirValueParameterImpl =
    BirValueParameterImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = UninitializedName,
        origin = IrDeclarationOrigin.DEFINED,
        type = BirUninitializedType,
        isAssignable = false,
        varargElementType = null,
        isCrossinline = false,
        isNoinline = false,
        isHidden = false,
        index = 0,
    ).apply(init)

@OptIn(ObsoleteDescriptorBasedAPI::class)
inline fun BirVariable.Companion.build(init: BirVariableImpl.() -> Unit): BirVariableImpl =
    BirVariableImpl(
        sourceSpan = SourceSpan.UNDEFINED,
        name = UninitializedName,
        origin = IrDeclarationOrigin.DEFINED,
        type = BirUninitializedType,
        isVar = false,
        isConst = false,
        isLateinit = false,
    ).apply(init)


inline fun BirBlockBody.Companion.build(init: BirBlockBodyImpl.() -> Unit): BirBlockBodyImpl =
    BirBlockBodyImpl(
        SourceSpan.UNDEFINED
    ).apply(init)


inline fun BirBlock.Companion.build(init: BirBlockImpl.() -> Unit): BirBlockImpl =
    BirBlockImpl(
        SourceSpan.UNDEFINED,
        type = BirUninitializedType,
        origin = null,
    ).apply(init)


inline fun BirCall.Companion.build(init: BirCallImpl.() -> Unit) =
    BirCallImpl(
        SourceSpan.UNDEFINED,
        type = BirUninitializedType,
        symbol = UninitializedBirSymbol.SimpleFunctionSymbol,
        dispatchReceiver = null,
        extensionReceiver = null,
        origin = null,
        typeArguments = emptyList(),
        contextReceiversCount = 0,
        superQualifierSymbol = null,
    ).apply(init)

inline fun BirConstructorCall.Companion.build(init: BirConstructorCallImpl.() -> Unit) =
    BirConstructorCallImpl(
        SourceSpan.UNDEFINED,
        type = BirUninitializedType,
        symbol = UninitializedBirSymbol.ConstructorSymbol,
        dispatchReceiver = null,
        extensionReceiver = null,
        origin = null,
        source = SourceElement.NO_SOURCE,
        typeArguments = emptyList(),
        constructorTypeArgumentsCount = 0,
        contextReceiversCount = 0,
    ).apply(init)

inline fun BirConst.Companion.build(init: BirConstImpl<Any?>.() -> Unit) =
    BirConstImpl<Any?>(
        SourceSpan.UNDEFINED,
        type = BirUninitializedType,
        kind = @Suppress("UNCHECKED_CAST") (IrConstKind.Null as IrConstKind<Any?>),
        value = null,
    ).apply(init)


@PublishedApi
internal val UninitializedName = Name.identifier("UNNAMED")

abstract class UninitializedBirSymbol<E : BirSymbolOwner>() : BirTypedSymbol<E> {
    final override val isBound: Boolean
        get() = false
    final override val owner: E
        get() = error("Uninitialzied")
    final override val signature: IdSignature
        get() = error("Uninitialzied")

    object FileSymbol : UninitializedBirSymbol<BirFile>(), BirFileSymbol
    object ExternalPackageFragmentSymbol : UninitializedBirSymbol<BirExternalPackageFragment>(), BirExternalPackageFragmentSymbol
    object AnonymousInitializerSymbol : UninitializedBirSymbol<BirAnonymousInitializer>(), BirAnonymousInitializerSymbol
    object EnumEntrySymbol : UninitializedBirSymbol<BirEnumEntry>(), BirEnumEntrySymbol
    object FieldSymbol : UninitializedBirSymbol<BirField>(), BirFieldSymbol
    object ClassSymbol : UninitializedBirSymbol<BirClass>(), BirClassSymbol
    object ScriptSymbol : UninitializedBirSymbol<BirScript>(), BirScriptSymbol
    object TypeParameterSymbol : UninitializedBirSymbol<BirTypeParameter>(), BirTypeParameterSymbol
    object ValueParameterSymbol : UninitializedBirSymbol<BirValueParameter>(), BirValueParameterSymbol
    object VariableSymbol : UninitializedBirSymbol<BirVariable>(), BirVariableSymbol
    object ConstructorSymbol : UninitializedBirSymbol<BirConstructor>(), BirConstructorSymbol
    object SimpleFunctionSymbol : UninitializedBirSymbol<BirSimpleFunction>(), BirSimpleFunctionSymbol
    object ReturnableBlockSymbol : UninitializedBirSymbol<BirReturnableBlock>(), BirReturnableBlockSymbol
    object PropertySymbol : UninitializedBirSymbol<BirProperty>(), BirPropertySymbol
    object LocalDelegatedPropertySymbol : UninitializedBirSymbol<BirLocalDelegatedProperty>(), BirLocalDelegatedPropertySymbol
    object TypeAliasSymbol : UninitializedBirSymbol<BirTypeAlias>(), BirTypeAliasSymbol
}
