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
import org.jetbrains.kotlin.backend.common.serialization.DescriptorUniqIdAware
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.UniqId
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
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
) : KotlinIrLinker(logger, builtIns, symbolTable, exportedDependencies, forwardModuleDescriptor, 0L),
    DescriptorUniqIdAware by KonanDescriptorUniqIdAware {

    override val descriptorReferenceDeserializer =
        KonanDescriptorReferenceDeserializer(currentModule, resolvedForwardDeclarations)

    override fun reader(moduleDescriptor: ModuleDescriptor, uniqId: UniqId) =
        moduleDescriptor.konanLibrary!!.irDeclaration(uniqId.index, uniqId.isLocal)

    override val ModuleDescriptor.irHeader get() = this.konanLibrary!!.irHeader

    val modules: Map<String, IrModuleFragment> get() = mutableMapOf<String, IrModuleFragment>().apply {
        deserializersForModules.forEach {
            this.put(it.key.konanLibrary!!.libraryName, it.value.module)
        }
    }
}
