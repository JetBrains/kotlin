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

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.backend.konan.llvm.KonanMangler
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable

class KonanIrLinker(
        currentModule: ModuleDescriptor,
        logger: LoggingContext,
        builtIns: IrBuiltIns,
        symbolTable: SymbolTable,
        forwardModuleDescriptor: ModuleDescriptor?,
        exportedDependencies: List<ModuleDescriptor>
) : KotlinIrLinker(logger, builtIns, symbolTable, exportedDependencies, forwardModuleDescriptor, KonanMangler),
    DescriptorUniqIdAware by DeserializedDescriptorUniqIdAware {

    override val descriptorReferenceDeserializer =
            KonanDescriptorReferenceDeserializer(currentModule, KonanMangler, builtIns, resolvedForwardDeclarations)

    override fun reader(moduleDescriptor: ModuleDescriptor, fileIndex: Int, uniqId: UniqId) =
            moduleDescriptor.konanLibrary!!.irDeclaration(uniqId.index, fileIndex)

    override fun readSymbol(moduleDescriptor: ModuleDescriptor, fileIndex: Int, symbolIndex: Int) =
            moduleDescriptor.konanLibrary!!.symbol(symbolIndex, fileIndex)

    override fun readType(moduleDescriptor: ModuleDescriptor, fileIndex: Int, typeIndex: Int) =
            moduleDescriptor.konanLibrary!!.type(typeIndex, fileIndex)

    override fun readString(moduleDescriptor: ModuleDescriptor, fileIndex: Int, stringIndex: Int) =
            moduleDescriptor.konanLibrary!!.string(stringIndex, fileIndex)

    override fun readBody(moduleDescriptor: ModuleDescriptor, fileIndex: Int, bodyIndex: Int) =
            moduleDescriptor.konanLibrary!!.body(bodyIndex, fileIndex)

    override fun readFile(moduleDescriptor: ModuleDescriptor, fileIndex: Int) =
            moduleDescriptor.konanLibrary!!.file(fileIndex)

    override fun readFileCount(moduleDescriptor: ModuleDescriptor) =
            moduleDescriptor.run { if (isForwardDeclarationModule) 0 else konanLibrary!!.fileCount() }

    private val ModuleDescriptor.userName get() = konanLibrary!!.libraryFile.absolutePath

    override fun checkAccessibility(declarationDescriptor: DeclarationDescriptor) = true

    override fun handleNoModuleDeserializerFound(key: UniqId): DeserializationState<*> {
        return globalDeserializationState
    }

    val modules: Map<String, IrModuleFragment> get() = mutableMapOf<String, IrModuleFragment>().apply {
        deserializersForModules.filter { !it.key.isForwardDeclarationModule }.forEach {
            this.put(it.key.konanLibrary!!.libraryName, it.value.module)
        }
    }
}
