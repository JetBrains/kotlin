package org.jetbrains.kotlin.bir.symbols

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirReturnableBlock
import org.jetbrains.kotlin.ir.util.IdSignature

abstract class ExternalBirSymbol<out E : BirSymbolOwner>(
    override val signature: IdSignature?,
) : BirTypedSymbol<E> {
    override val owner: E
        get() = error("The symbol is not bound")
    override val isBound: Boolean
        get() = false

    class FileSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirFile>(signature), BirFileSymbol

    class ExternalPackageFragmentSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirExternalPackageFragment>(signature), BirExternalPackageFragmentSymbol

    class AnonymousInitializerSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirAnonymousInitializer>(signature), BirAnonymousInitializerSymbol

    class EnumEntrySymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirEnumEntry>(signature), BirEnumEntrySymbol

    class FieldSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirField>(signature), BirFieldSymbol

    class ClassSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirClass>(signature), BirClassSymbol

    class ScriptSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirScript>(signature), BirScriptSymbol

    class TypeParameterSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirTypeParameter>(signature), BirTypeParameterSymbol

    class ValueParameterSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirValueParameter>(signature), BirValueParameterSymbol

    class VariableSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirVariable>(signature), BirVariableSymbol

    class ConstructorSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirConstructor>(signature), BirConstructorSymbol

    class SimpleFunctionSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirSimpleFunction>(signature), BirSimpleFunctionSymbol

    class ReturnableBlockSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirReturnableBlock>(signature), BirReturnableBlockSymbol

    class PropertySymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirProperty>(signature), BirPropertySymbol

    class LocalDelegatedPropertySymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirLocalDelegatedProperty>(signature), BirLocalDelegatedPropertySymbol

    class TypeAliasSymbol(signature: IdSignature?) :
        ExternalBirSymbol<BirTypeAlias>(signature), BirTypeAliasSymbol
}