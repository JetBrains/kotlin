/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.factories.IrDeclarationFactory
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class ExternalDependenciesGenerator(val symbolTable: SymbolTable, private val irProviders: List<IrProvider>) {
    fun generateUnboundSymbolsAsDependencies() {
        // There should be at most one DeclarationStubGenerator (none in closed world?)
        irProviders.singleOrNull { it is DeclarationStubGenerator }?.let {
            (it as DeclarationStubGenerator).unboundSymbolGeneration = true
        }
        /*
            Deserializing a reference may lead to new unbound references, so we loop until none are left.
         */
        lateinit var unbound: List<IrSymbol>
        do {
            unbound = symbolTable.allUnbound

            for (symbol in unbound) {
                // Symbol could get bound as a side effect of deserializing other symbols.
                if (!symbol.isBound) {
                    irProviders.getDeclaration(symbol)
                }
                assert(symbol.isBound) { "$symbol unbound even after deserialization attempt" }
            }
        } while (unbound.isNotEmpty())

        irProviders.forEach { (it as? IrDeserializer)?.declareForwardDeclarations() }
    }
}

private val SymbolTable.allUnbound: List<IrSymbol>
    get() {
        val r = mutableListOf<IrSymbol>()
        r.addAll(unboundClasses)
        r.addAll(unboundConstructors)
        r.addAll(unboundEnumEntries)
        r.addAll(unboundFields)
        r.addAll(unboundSimpleFunctions)
        r.addAll(unboundProperties)
        r.addAll(unboundTypeParameters)
        r.addAll(unboundTypeAliases)
        return r
    }

fun List<IrProvider>.getDeclaration(symbol: IrSymbol): IrDeclaration =
    firstNotNullResult { provider ->
        provider.getDeclaration(symbol)
    } ?: error("Could not find declaration for unbound symbol $symbol")

// In most cases, IrProviders list consist of an optional deserializer and a DeclarationStubGenerator.
fun generateTypicalIrProviderList(
    moduleDescriptor: ModuleDescriptor,
    irBuiltins: IrBuiltIns,
    symbolTable: SymbolTable,
    deserializer: IrDeserializer? = null,
    extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY,
    irDeclarationFactory: IrDeclarationFactory = IrDeclarationFactory.DEFAULT
): List<IrProvider> {
    val stubGenerator = DeclarationStubGenerator(
        moduleDescriptor, symbolTable, irBuiltins.languageVersionSettings, extensions,
        irDeclarationFactory
    )
    return listOfNotNull(deserializer, stubGenerator).also {
        stubGenerator.setIrProviders(it)
    }
}