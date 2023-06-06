/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.symbols

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

/**
 * A special object that can be used to refer to [IrDeclaration]s and some other entities from IR nodes.
 *
 * For example, [IrCall] uses [IrSimpleFunctionSymbol] to refer to the [IrSimpleFunction] that is being called.
 *
 * **Q:** Why not just use the [IrSimpleFunction] class itself?
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
interface IrSymbol : DeclarationSymbolMarker {

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
    val owner: IrSymbolOwner

    /**
     * If [hasDescriptor] is `true`, returns the [DeclarationDescriptor] of the declaration that this symbol was created for.
     * Otherwise, returns a dummy [IrBasedDeclarationDescriptor] that serves as a descriptor-like view to [owner].
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
val IrSymbol.isPublicApi: Boolean
    get() = signature != null

/**
 * A stricter-typed [IrSymbol] that allows to set the owner using the [bind] method. The owner can be set only once.
 *
 * In fact, any [IrSymbol] is [IrBindableSymbol], but having a non-generic interface like [IrSymbol] is sometimes useful.
 *
 * Only leaf interfaces in the symbol hierarchy inherit from this interface.
 */
interface IrBindableSymbol<out Descriptor : DeclarationDescriptor, Owner : IrSymbolOwner> : IrSymbol {
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
 * A symbol whose [owner] is either [IrFile] or [IrExternalPackageFragment].
 */
sealed interface IrPackageFragmentSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: PackageFragmentDescriptor
}

/**
 * A symbol whose [owner] is [IrFile]. Such a symbol is always module-private.
 *
 * [IrFileSymbol] is never actually serialized, but is useful for deserializing private top-level declarations.
 *
 * @see IdSignature.FileSignature
 */
interface IrFileSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrFile>

/**
 * A symbol whose [owner] is [IrExternalPackageFragment].
 */
interface IrExternalPackageFragmentSymbol : IrPackageFragmentSymbol, IrBindableSymbol<PackageFragmentDescriptor, IrExternalPackageFragment>

/**
 * A symbol whose [owner] is [IrAnonymousInitializer].
 *
 * It's not very useful on its own, but since [IrAnonymousInitializer] is an [IrDeclaration], and [IrDeclaration]s must have symbols,
 * here we are.
 *
 * This symbol is never public (wrt linkage).
 */
interface IrAnonymousInitializerSymbol : IrBindableSymbol<ClassDescriptor, IrAnonymousInitializer>

/**
 * A symbol whose [owner] is [IrEnumEntry].
 *
 * @see IrGetEnumValue
 */
interface IrEnumEntrySymbol : IrBindableSymbol<ClassDescriptor, IrEnumEntry>, EnumEntrySymbolMarker

/**
 * A symbol whose [owner] is [IrField].
 *
 * @see IrGetField
 * @see IrSetField
 */
interface IrFieldSymbol : IrBindableSymbol<PropertyDescriptor, IrField>, FieldSymbolMarker

/**
 * A symbol whose [owner] is [IrClass], [IrScript] or [IrTypeParameter].
 *
 * @see IrSimpleType
 * @see IrClassReference
 */
sealed interface IrClassifierSymbol : IrSymbol, TypeConstructorMarker {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassifierDescriptor
}

/**
 * A symbol whose [owner] is [IrClass].
 *
 * @see IrClass.sealedSubclasses
 * @see IrCall.superQualifierSymbol
 * @see IrFieldAccessExpression.superQualifierSymbol
 */
interface IrClassSymbol : IrClassifierSymbol, IrBindableSymbol<ClassDescriptor, IrClass>, RegularClassSymbolMarker

/**
 * A symbol whose [owner] is [IrScript].
 */
interface IrScriptSymbol : IrClassifierSymbol, IrBindableSymbol<ScriptDescriptor, IrScript>

/**
 * A symbol whose [owner] is [IrTypeParameter].
 */
interface IrTypeParameterSymbol : IrClassifierSymbol,
    IrBindableSymbol<TypeParameterDescriptor, IrTypeParameter>,
    TypeParameterMarker,
    TypeParameterSymbolMarker

/**
 * A symbol whose [owner] is [IrValueParameter] or [IrVariable].
 *
 * @see IrGetValue
 * @see IrSetValue
 */
sealed interface IrValueSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ValueDescriptor

    override val owner: IrValueDeclaration
}

/**
 * A symbol whose [owner] is [IrValueParameter].
 */
interface IrValueParameterSymbol : IrValueSymbol, IrBindableSymbol<ParameterDescriptor, IrValueParameter>, ValueParameterSymbolMarker

/**
 * A symbol whose [owner] is [IrVariable].
 */
interface IrVariableSymbol : IrValueSymbol, IrBindableSymbol<VariableDescriptor, IrVariable>

/**
 * A symbol whose [owner] is [IrFunction] or [IrReturnableBlock].
 *
 * @see IrReturn
 */
sealed interface IrReturnTargetSymbol : IrSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor

    override val owner: IrReturnTarget
}

/**
 * A symbol whose [owner] is [IrConstructor] or [IrSimpleFunction].
 *
 * @see IrFunctionReference
 */
sealed interface IrFunctionSymbol : IrReturnTargetSymbol, FunctionSymbolMarker {
    override val owner: IrFunction
}

/**
 * A symbol whose [owner] is [IrConstructor].
 *
 * @see IrConstructorCall
 */
interface IrConstructorSymbol : IrFunctionSymbol, IrBindableSymbol<ClassConstructorDescriptor, IrConstructor>, ConstructorSymbolMarker

/**
 * A symbol whose [owner] is [IrSimpleFunction].
 *
 * @see IrCall
 */
interface IrSimpleFunctionSymbol : IrFunctionSymbol, IrBindableSymbol<FunctionDescriptor, IrSimpleFunction>, SimpleFunctionSymbolMarker

/**
 * A symbol whose [owner] is [IrReturnableBlock].
 */
interface IrReturnableBlockSymbol : IrReturnTargetSymbol, IrBindableSymbol<FunctionDescriptor, IrReturnableBlock>

/**
 * A symbol whose [owner] is [IrProperty].
 */
interface IrPropertySymbol : IrBindableSymbol<PropertyDescriptor, IrProperty>, PropertySymbolMarker

/**
 * A symbol whose [owner] is [IrLocalDelegatedProperty].
 */
interface IrLocalDelegatedPropertySymbol : IrBindableSymbol<VariableDescriptorWithAccessors, IrLocalDelegatedProperty>

/**
 * A symbol whose [owner] is [IrTypeAlias].
 *
 * @see IrTypeAbbreviation
 */
interface IrTypeAliasSymbol : IrBindableSymbol<TypeAliasDescriptor, IrTypeAlias>, TypeAliasSymbolMarker
