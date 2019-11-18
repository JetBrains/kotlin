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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import kotlin.math.min

class ExternalDependenciesGenerator(
    moduleDescriptor: ModuleDescriptor,
    val symbolTable: SymbolTable,
    val irBuiltIns: IrBuiltIns,
    private val deserializer: IrDeserializer? = null,
    irProviders: List<IrProvider> = emptyList(),
    extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY
) {
    private val stubGenerator = DeclarationStubGenerator(
        moduleDescriptor, symbolTable, irBuiltIns.languageVersionSettings, listOfNotNull(deserializer) + irProviders, extensions
    )

    fun generateUnboundSymbolsAsDependencies() {
        stubGenerator.unboundSymbolGeneration = true
        do {
            fun <T> haveNotStabilized(prev: ArrayList<T>, cur: Set<T>) =
                cur.isNotEmpty() && (prev.size != cur.size || prev.any { !cur.contains(it) })

            val unboundClasses = ArrayList(symbolTable.unboundClasses)
            val unboundConstructors = ArrayList(symbolTable.unboundConstructors)
            val unboundEnumEntries = ArrayList(symbolTable.unboundEnumEntries)
            val unboundFields = ArrayList(symbolTable.unboundFields)
            val unboundSimpleFunctions = ArrayList(symbolTable.unboundSimpleFunctions)
            val unboundProperties = ArrayList(symbolTable.unboundProperties)
            val unboundTypeParameters = ArrayList(symbolTable.unboundTypeParameters)
            val unboundTypeAliases = ArrayList(symbolTable.unboundTypeAliases)
            unboundClasses.forEach { stubGenerator.generateClassStub(it.descriptor) }
            unboundConstructors.forEach { stubGenerator.generateConstructorStub(it.descriptor) }
            unboundEnumEntries.forEach { stubGenerator.generateEnumEntryStub(it.descriptor) }
            unboundFields.forEach { stubGenerator.generateFieldStub(it.descriptor) }
            unboundSimpleFunctions.forEach { stubGenerator.generateFunctionStub(it.descriptor) }
            unboundProperties.forEach { stubGenerator.generatePropertyStub(it.descriptor) }
            unboundTypeParameters.forEach { stubGenerator.generateOrGetTypeParameterStub(it.descriptor) }
            unboundTypeAliases.forEach { stubGenerator.generateTypeAliasStub(it.descriptor) }
        } while (haveNotStabilized(unboundClasses, symbolTable.unboundClasses)
                || haveNotStabilized(unboundConstructors, symbolTable.unboundConstructors)
                || haveNotStabilized(unboundEnumEntries, symbolTable.unboundEnumEntries)
                || haveNotStabilized(unboundFields, symbolTable.unboundFields)
                || haveNotStabilized(unboundSimpleFunctions, symbolTable.unboundSimpleFunctions)
                || haveNotStabilized(unboundProperties, symbolTable.unboundProperties)
                || haveNotStabilized(unboundTypeParameters, symbolTable.unboundTypeParameters)
                || haveNotStabilized(unboundTypeAliases, symbolTable.unboundTypeAliases)
        )

        deserializer?.declareForwardDeclarations()

        assertEmpty(symbolTable.unboundClasses, "classes")
        assertEmpty(symbolTable.unboundConstructors, "constructors")
        assertEmpty(symbolTable.unboundEnumEntries, "enum entries")
        assertEmpty(symbolTable.unboundFields, "fields")
        assertEmpty(symbolTable.unboundSimpleFunctions, "simple functions")
        assertEmpty(symbolTable.unboundProperties, "properties")
        assertEmpty(symbolTable.unboundTypeParameters, "type parameters")
        assertEmpty(symbolTable.unboundTypeAliases, "type aliases")
    }

    private fun assertEmpty(s: Set<IrSymbol>, marker: String) {
        assert(s.isEmpty()) {
            "$marker: ${s.size} unbound:\n" +
                    s.toList().subList(0, min(10, s.size)).joinToString(separator = "\n") { it.descriptor.toString() }
        }
    }
}
