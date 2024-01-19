/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.symbols

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirReturnableBlock
import org.jetbrains.kotlin.ir.util.IdSignature

abstract class LateBoundBirSymbol<E : BirSymbolOwner>(
    override val signature: IdSignature?,
) : BirSymbol<E> {
    private var _owner: E? = null
    override val owner: E
        get() = _owner ?: error("The symbol is not yet bound")
    override val isBound: Boolean
        get() = _owner != null

    fun bindTo(element: E) {
        require(!isBound) { "This symbol is already bound to $element" }
        _owner = element
    }

    final override fun hashCode(): Int {
        return _owner?.hashCode() ?: super.hashCode()
    }

    final override fun equals(other: Any?): Boolean {
        // The owner of this symbol may be an BirSymbol itself
        return other === this || (other != null && other === _owner)
    }

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class FileSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirFile>(signature), BirFileSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class ExternalPackageFragmentSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirExternalPackageFragment>(signature), BirExternalPackageFragmentSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class AnonymousInitializerSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirAnonymousInitializer>(signature), BirAnonymousInitializerSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class EnumEntrySymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirEnumEntry>(signature), BirEnumEntrySymbol

    class FieldSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirField>(signature), BirFieldSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class ClassSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirClass>(signature), BirClassSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class ScriptSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirScript>(signature), BirScriptSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class TypeParameterSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirTypeParameter>(signature), BirTypeParameterSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class ValueParameterSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirValueParameter>(signature), BirValueParameterSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class VariableSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirVariable>(signature), BirVariableSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class ConstructorSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirConstructor>(signature), BirConstructorSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class SimpleFunctionSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirSimpleFunction>(signature), BirSimpleFunctionSymbol

    @Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
    class ReturnableBlockSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirReturnableBlock>(signature), BirReturnableBlockSymbol

    class PropertySymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirProperty>(signature), BirPropertySymbol

    class LocalDelegatedPropertySymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirLocalDelegatedProperty>(signature), BirLocalDelegatedPropertySymbol

    class TypeAliasSymbol(signature: IdSignature?) :
        LateBoundBirSymbol<BirTypeAlias>(signature), BirTypeAliasSymbol
}