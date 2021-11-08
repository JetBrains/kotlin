/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.lazy.LazyScopedTypeParametersResolver
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*

class DeclarationStubGeneratorImpl(
    moduleDescriptor: ModuleDescriptor,
    symbolTable: SymbolTable,
    irBuiltins: IrBuiltIns,
    mangler: KotlinMangler.DescriptorMangler,
    extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY,
) : DeclarationStubGenerator(moduleDescriptor, symbolTable, irBuiltins, extensions) {
    override val typeTranslator: TypeTranslator =
        TypeTranslatorImpl(
            lazyTable,
            irBuiltins.languageVersionSettings,
            moduleDescriptor,
            { LazyScopedTypeParametersResolver(lazyTable) },
            true,
            extensions
        )

    override val descriptorFinder: DescriptorByIdSignatureFinder = DescriptorByIdSignatureFinderImpl(
        moduleDescriptor,
        mangler,
        DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_WITH_DEPENDENCIES
    )
}

// In most cases, IrProviders list consist of an optional deserializer and a DeclarationStubGenerator.
fun generateTypicalIrProviderList(
    moduleDescriptor: ModuleDescriptor,
    irBuiltins: IrBuiltIns,
    symbolTable: SymbolTable,
    mangler: KotlinMangler.DescriptorMangler,
    deserializer: IrDeserializer? = null,
    extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY
): List<IrProvider> {
    val stubGenerator = DeclarationStubGeneratorImpl(
        moduleDescriptor, symbolTable, irBuiltins, mangler, extensions
    )
    return listOfNotNull(deserializer, stubGenerator)
}


@OptIn(ObsoleteDescriptorBasedAPI::class)
class DeclarationStubGeneratorForNotFoundClasses(
    private val stubGenerator: DeclarationStubGeneratorImpl
) : IrProvider {

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        if (symbol.isBound) return null

        val classDescriptor = symbol.descriptor as? NotFoundClasses.MockClassDescriptor
            ?: return null
        return stubGenerator.generateClassStub(classDescriptor)
    }
}
