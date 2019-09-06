package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

class KonanDescriptorReferenceDeserializer(
    currentModule: ModuleDescriptor,
    mangler: KotlinMangler,
    builtIns: IrBuiltIns,
    resolvedForwardDeclarations: MutableMap<UniqId, UniqId>
): DescriptorReferenceDeserializer(currentModule, mangler, builtIns, resolvedForwardDeclarations),
   DescriptorUniqIdAware by DeserializedDescriptorUniqIdAware
