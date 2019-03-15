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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class ExternalDependenciesGenerator(
    moduleDescriptor: ModuleDescriptor,
    val symbolTable: SymbolTable,
    val irBuiltIns: IrBuiltIns,
    externalDeclarationOrigin: ((DeclarationDescriptor) -> IrDeclarationOrigin)? = null,
    private val deserializer: IrDeserializer? = null
) {
    private val stubGenerator = DeclarationStubGenerator(
        moduleDescriptor, symbolTable, irBuiltIns.languageVersionSettings, externalDeclarationOrigin, deserializer
    )

    fun generateUnboundSymbolsAsDependencies() {
        stubGenerator.unboundSymbolGeneration = true
        ArrayList(symbolTable.unboundClasses).forEach {
            stubGenerator.generateClassStub(it.descriptor)
        }
        ArrayList(symbolTable.unboundConstructors).forEach {
            stubGenerator.generateConstructorStub(it.descriptor)
        }
        ArrayList(symbolTable.unboundEnumEntries).forEach {
            stubGenerator.generateEnumEntryStub(it.descriptor)
        }
        ArrayList(symbolTable.unboundFields).forEach {
            stubGenerator.generateFieldStub(it.descriptor)
        }
        ArrayList(symbolTable.unboundSimpleFunctions).forEach {
            stubGenerator.generateFunctionStub(it.descriptor)
        }
        ArrayList(symbolTable.unboundTypeParameters).forEach {
            stubGenerator.generateOrGetTypeParameterStub(it.descriptor)
        }

        deserializer?.declareForwardDeclarations()

        if (deserializer != null) return

        assert(symbolTable.unboundClasses.isEmpty())
        assert(symbolTable.unboundConstructors.isEmpty())
        assert(symbolTable.unboundEnumEntries.isEmpty())
        assert(symbolTable.unboundFields.isEmpty())
        assert(symbolTable.unboundSimpleFunctions.isEmpty())
        assert(symbolTable.unboundTypeParameters.isEmpty())
    }
}
