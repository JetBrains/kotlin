/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.lazy.LazyScopedTypeParametersResolver
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator

class DeclarationStubGeneratorImpl(
    moduleDescriptor: ModuleDescriptor,
    symbolTable: SymbolTable,
    languageVersionSettings: LanguageVersionSettings,
    extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY,
) : DeclarationStubGenerator(moduleDescriptor, symbolTable, extensions) {
    override val typeTranslator: TypeTranslator =
        TypeTranslatorImpl(
            lazyTable,
            symbolTable.signaturer,
            languageVersionSettings,
            moduleDescriptor,
            { LazyScopedTypeParametersResolver(lazyTable) },
            true,
            extensions
        )
}

// In most cases, IrProviders list consist of an optional deserializer and a DeclarationStubGenerator.
fun generateTypicalIrProviderList(
    moduleDescriptor: ModuleDescriptor,
    irBuiltins: IrBuiltIns,
    symbolTable: SymbolTable,
    deserializer: IrDeserializer? = null,
    extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY
): List<IrProvider> {
    val stubGenerator = DeclarationStubGeneratorImpl(
        moduleDescriptor, symbolTable, irBuiltins.languageVersionSettings, extensions
    )
    return listOfNotNull(deserializer, stubGenerator)
}
