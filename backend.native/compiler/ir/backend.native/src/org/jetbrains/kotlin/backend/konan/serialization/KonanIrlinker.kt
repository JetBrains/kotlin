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
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KonanIrLinker(
    private val currentModule: ModuleDescriptor,
    logger: LoggingContext,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable,
    private val forwardModuleDescriptor: ModuleDescriptor?,
    exportedDependencies: List<ModuleDescriptor>
) : KotlinIrLinker(logger, builtIns, symbolTable, exportedDependencies, forwardModuleDescriptor) {

    private val descriptorByIdSignatureFinder = DescriptorByIdSignatureFinder(currentModule)

    override fun reader(moduleDescriptor: ModuleDescriptor, fileIndex: Int, idSigIndex: Int) =
            moduleDescriptor.konanLibrary!!.irDeclaration(idSigIndex, fileIndex)

    override fun readType(moduleDescriptor: ModuleDescriptor, fileIndex: Int, typeIndex: Int) =
            moduleDescriptor.konanLibrary!!.type(typeIndex, fileIndex)

    override fun readSignature(moduleDescriptor: ModuleDescriptor, fileIndex: Int, signatureIndex: Int) =
            moduleDescriptor.konanLibrary!!.signature(signatureIndex, fileIndex)

    override fun readString(moduleDescriptor: ModuleDescriptor, fileIndex: Int, stringIndex: Int) =
            moduleDescriptor.konanLibrary!!.string(stringIndex, fileIndex)

    override fun readBody(moduleDescriptor: ModuleDescriptor, fileIndex: Int, bodyIndex: Int) =
            moduleDescriptor.konanLibrary!!.body(bodyIndex, fileIndex)

    override fun readFile(moduleDescriptor: ModuleDescriptor, fileIndex: Int) =
            moduleDescriptor.konanLibrary!!.file(fileIndex)

    override fun readFileCount(moduleDescriptor: ModuleDescriptor) =
            moduleDescriptor.run { if (this === forwardModuleDescriptor || moduleDescriptor.isFromInteropLibrary()) 0 else konanLibrary!!.fileCount() }

    override fun handleNoModuleDeserializerFound(idSignature: IdSignature): DeserializationState<*> {
        return globalDeserializationState
    }

    companion object {
        private val C_NAMES_NAME = Name.identifier("cnames")
        private val OBJC_NAMES_NAME = Name.identifier("objcnames")
    }

    override fun isSpecialPlatformSignature(idSig: IdSignature): Boolean = idSig.isForwardDeclarationSignature()

    override fun postProcessPlatformSpecificDeclaration(idSig: IdSignature, descriptor: DeclarationDescriptor?, block: (IdSignature) -> Unit) {
        if (descriptor == null) return

        if (!idSig.isForwardDeclarationSignature()) return

        val fqn = descriptor.fqNameSafe
        if (!fqn.startsWith(C_NAMES_NAME) && !fqn.startsWith(OBJC_NAMES_NAME)) {
            val signature = IdSignature.PublicSignature(fqn.parent(), FqName(fqn.shortName().asString()), null, 0)
            block(signature)
        }
    }

    override fun resolvePlatformDescriptor(idSig: IdSignature): DeclarationDescriptor? {
        if (idSig.isInteropSignature()) {
            return descriptorByIdSignatureFinder.findDescriptorBySignature(idSig)
        }
        if (!idSig.isForwardDeclarationSignature()) return null

        val fwdModule = forwardModuleDescriptor ?: error("Forward declaration module should not be null")

        return with(idSig as IdSignature.PublicSignature) {
            val classId = ClassId(packageFqn, declarationFqn, false)
            fwdModule.findClassAcrossModuleDependencies(classId)
        }
    }

    private fun IdSignature.isInteropSignature(): Boolean = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()

    private fun IdSignature.isForwardDeclarationSignature(): Boolean {
        if (isPublic) {
            return packageFqName().run {
                startsWith(C_NAMES_NAME) || startsWith(OBJC_NAMES_NAME)
            }
        }

        return false
    }

    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, strategy: DeserializationStrategy): IrModuleDeserializer {
        return KonanModuleDeserializer(moduleDescriptor, strategy)
    }

    private inner class KonanModuleDeserializer(moduleDescriptor: ModuleDescriptor, strategy: DeserializationStrategy):
        IrModuleDeserializer(moduleDescriptor, strategy)

    /**
     * If declaration is from interop library then IR for it is generated by IrProviderForInteropStubs.
     */
    override fun IdSignature.shouldBeDeserialized(): Boolean = !isInteropSignature() && !isForwardDeclarationSignature()

    val modules: Map<String, IrModuleFragment> get() = mutableMapOf<String, IrModuleFragment>().apply {
        deserializersForModules.filter { !it.key.isForwardDeclarationModule }.forEach {
            this.put(it.key.konanLibrary!!.libraryName, it.value.module)
        }
    }
}
