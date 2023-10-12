/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.symbols

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrSymbolInternals
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

/**
 * A special object that can be used to refer to [BirDeclaration]s and some other entities from IR nodes.
 *
 * For example, [BirCall] uses [BirSimpleFunctionSymbol] to refer to the [BirSimpleFunction] that is being called.
 *
 * **Q:** Why not just use the [BirSimpleFunction] class itself?
 *
 * **A:** Because a symbol, unlike a declaration, can be bound or unbound (see [isBound]).
 *
 * We need this distinction to work with IR before the linkage phase. In pre-linkage IR the symbols referencing declarations from
 * other modules are not yet bound.
 *
 * During the linkage phase, we collect all the unbound symbols in the IR tree and try to resolve them to the declarations they should
 * refer to. For unbound symbols for public declarations from other modules, [signature] is used to resolve those declarations.
 *
 * @see IdSignature
 * @see SymbolTable
 */
interface BirSymbol : DeclarationSymbolMarker {

    /**
     * The declaration that this symbol refers to if it's bound.
     *
     * If the symbol is unbound, throws [IllegalStateException].
     *
     * **Q:** Why we didn't make this property nullable instead of throwing an exception?
     *
     * **A:** Because we most often need to access a symbol's owner in lowerings, which happen after linkage, at which point all symbols
     * should be already bound. Declaring this property nullable would make working with it more difficult most of the time.
     */
    @IrSymbolInternals
    val owner: BirSymbolOwner

    /**
     * If [hasDescriptor] is `true`, returns the [DeclarationDescriptor] of the declaration that this symbol was created for.
     * Otherwise, returns a dummy [BirBasedDeclarationDescriptor] that serves as a descriptor-like view to [owner].
     */
    @ObsoleteDescriptorBasedAPI
    val descriptor: DeclarationDescriptor

    /**
     * Returns `true` if this symbol was created from a [DeclarationDescriptor] either emitted by the K1 (aka classic) frontend,
     * or from deserialized metadata.
     *
     * @see descriptor
     */
    @ObsoleteDescriptorBasedAPI
    val hasDescriptor: Boolean

    /**
     * Whether this symbol has already been resolved to its [owner].
     */
    val isBound: Boolean

    /**
     * If this symbol refers to a publicly accessible declaration (from the binary artifact point of view),
     * returns the binary signature of that declaration.
     *
     * Otherwise, returns `null`.
     *
     * @see IdSignature.isPubliclyVisible
     */
    val signature: IdSignature?

    // TODO: remove once JS IR IC migrates to a different stable tag generation scheme
    // Used to store signatures in private symbols for JS IC
    /**
     * If this symbol refers to a local declaration, the signature of that declaration, otherwise `null`.
     *
     * @see IdSignature.isPubliclyVisible
     */
    var privateSignature: IdSignature?
}

/**
 * Whether this symbol refers to a publicly accessible declaration (from the binary artifact point of view).
 *
 * The symbol doesn't have to be bound.
 */
val BirSymbol.isPublicApi: Boolean
    get() = signature != null

/**
 * A stricter-typed [BirSymbol] that allows to set the owner using the [bind] method. The owner can be set only once.
 *
 * In fact, any [BirSymbol] is [BirBindableSymbol], but having a non-generic interface like [BirSymbol] is sometimes useful.
 *
 * Only leaf interfaces in the symbol hierarchy inherit from this interface.
 */
interface BirBindableSymbol<out Descriptor : DeclarationDescriptor, Owner : BirSymbolOwner> : BirSymbol {
    @IrSymbolInternals
    override val owner: Owner

    @ObsoleteDescriptorBasedAPI
    override val descriptor: Descriptor

    /**
     * Sets this symbol's owner.
     *
     * Throws [IllegalStateException] if this symbol has already been bound.
     */
    fun bind(owner: Owner)
}

/**
 * A symbol whose [owner] is either [BirFile] or [BirExternalPackageFragment].
 */
sealed interface BirPackageFragmentSymbol : BirSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: PackageFragmentDescriptor
}

/**
 * A symbol whose [owner] is [BirFile]. Such a symbol is always module-private.
 *
 * [BirFileSymbol] is never actually serialized, but is useful for deserializing private top-level declarations.
 *
 * @see IdSignature.FileSignature
 */
interface BirFileSymbol : BirPackageFragmentSymbol, BirBindableSymbol<PackageFragmentDescriptor, BirFile>

/**
 * A symbol whose [owner] is [BirExternalPackageFragment].
 */
interface BirExternalPackageFragmentSymbol : BirPackageFragmentSymbol, BirBindableSymbol<PackageFragmentDescriptor, BirExternalPackageFragment>

/**
 * A symbol whose [owner] is [BirAnonymousInitializer].
 *
 * It's not very useful on its own, but since [BirAnonymousInitializer] is an [BirDeclaration], and [BirDeclaration]s must have symbols,
 * here we are.
 *
 * This symbol is never public (wrt linkage).
 */
interface BirAnonymousInitializerSymbol : BirBindableSymbol<ClassDescriptor, BirAnonymousInitializer>

/**
 * A symbol whose [owner] is [BirEnumEntry].
 *
 * @see BirGetEnumValue
 */
interface BirEnumEntrySymbol : BirBindableSymbol<ClassDescriptor, BirEnumEntry>, EnumEntrySymbolMarker

/**
 * A symbol whose [owner] is [BirField].
 *
 * @see BirGetField
 * @see BirSetField
 */
interface BirFieldSymbol : BirBindableSymbol<PropertyDescriptor, BirField>, FieldSymbolMarker

/**
 * A symbol whose [owner] is [BirClass], [BirScript] or [BirTypeParameter].
 *
 * @see BirSimpleType
 * @see BirClassReference
 */
sealed interface BirClassifierSymbol : BirSymbol, TypeConstructorMarker {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassifierDescriptor
}

/**
 * A symbol whose [owner] is [BirClass].
 *
 * @see BirClass.sealedSubclasses
 * @see BirCall.superQualifierSymbol
 * @see BirFieldAccessExpression.superQualifierSymbol
 */
interface BirClassSymbol : BirClassifierSymbol, BirBindableSymbol<ClassDescriptor, BirClass>, RegularClassSymbolMarker

/**
 * A symbol whose [owner] is [BirScript].
 */
interface BirScriptSymbol : BirClassifierSymbol, BirBindableSymbol<ScriptDescriptor, BirScript>

/**
 * A symbol whose [owner] is [BirTypeParameter].
 */
interface BirTypeParameterSymbol : BirClassifierSymbol,
    BirBindableSymbol<TypeParameterDescriptor, BirTypeParameter>,
    TypeParameterMarker,
    TypeParameterSymbolMarker

/**
 * A symbol whose [owner] is [BirValueParameter] or [BirVariable].
 *
 * @see BirGetValue
 * @see BirSetValue
 */
sealed interface BirValueSymbol : BirSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ValueDescriptor

    @IrSymbolInternals
    override val owner: BirValueDeclaration
}

/**
 * A symbol whose [owner] is [BirValueParameter].
 */
interface BirValueParameterSymbol : BirValueSymbol, BirBindableSymbol<ParameterDescriptor, BirValueParameter>, ValueParameterSymbolMarker

/**
 * A symbol whose [owner] is [BirVariable].
 */
interface BirVariableSymbol : BirValueSymbol, BirBindableSymbol<VariableDescriptor, BirVariable>

/**
 * A symbol whose [owner] is [BirFunction] or [BirReturnableBlock].
 *
 * @see BirReturn
 */
sealed interface BirReturnTargetSymbol : BirSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor

    @IrSymbolInternals
    override val owner: BirReturnTarget
}

/**
 * A symbol whose [owner] is [BirConstructor] or [BirSimpleFunction].
 *
 * @see BirFunctionReference
 */
sealed interface BirFunctionSymbol : BirReturnTargetSymbol, FunctionSymbolMarker {
    @IrSymbolInternals
    override val owner: BirFunction
}

/**
 * A symbol whose [owner] is [BirConstructor].
 *
 * @see BirConstructorCall
 */
interface BirConstructorSymbol : BirFunctionSymbol, BirBindableSymbol<ClassConstructorDescriptor, BirConstructor>, ConstructorSymbolMarker

/**
 * A symbol whose [owner] is [BirSimpleFunction].
 *
 * @see BirCall
 */
interface BirSimpleFunctionSymbol : BirFunctionSymbol, BirBindableSymbol<FunctionDescriptor, BirSimpleFunction>, SimpleFunctionSymbolMarker

/**
 * A symbol whose [owner] is [BirReturnableBlock].
 */
interface BirReturnableBlockSymbol : BirReturnTargetSymbol, BirBindableSymbol<FunctionDescriptor, BirReturnableBlock>

/**
 * A symbol whose [owner] is [BirProperty].
 */
interface BirPropertySymbol : BirBindableSymbol<PropertyDescriptor, BirProperty>, PropertySymbolMarker

/**
 * A symbol whose [owner] is [BirLocalDelegatedProperty].
 */
interface BirLocalDelegatedPropertySymbol : BirBindableSymbol<VariableDescriptorWithAccessors, BirLocalDelegatedProperty>

/**
 * A symbol whose [owner] is [BirTypeAlias].
 *
 * @see BirTypeAbbreviation
 */
interface BirTypeAliasSymbol : BirBindableSymbol<TypeAliasDescriptor, BirTypeAlias>, TypeAliasSymbolMarker
