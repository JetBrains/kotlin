/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.symbols

import org.jetbrains.kotlin.ir.util.IdSignature

abstract class ExternalBirSymbol(
    override val signature: IdSignature?,
) : BirSymbol {
    class FileSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirFileSymbol
    class ExternalPackageFragmentSymbol(signature: IdSignature?) :
        ExternalBirSymbol(signature), BirExternalPackageFragmentSymbol

    class AnonymousInitializerSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirAnonymousInitializerSymbol

    class EnumEntrySymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirEnumEntrySymbol
    class FieldSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirFieldSymbol
    class ClassSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirClassSymbol
    class ScriptSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirScriptSymbol
    class TypeParameterSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirTypeParameterSymbol

    class ValueParameterSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirValueParameterSymbol

    class VariableSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirVariableSymbol
    class ConstructorSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirConstructorSymbol

    class SimpleFunctionSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirSimpleFunctionSymbol

    class ReturnableBlockSymbol(signature: IdSignature?) : ExternalBirSymbol(signature),
        BirReturnableBlockSymbol

    class PropertySymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirPropertySymbol
    class LocalDelegatedPropertySymbol(signature: IdSignature?) :
        ExternalBirSymbol(signature), BirLocalDelegatedPropertySymbol

    class TypeAliasSymbol(signature: IdSignature?) : ExternalBirSymbol(signature), BirTypeAliasSymbol
}
