/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.metadata.KonanIr
import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite

// This is an abstract uniqIdIndex any serialized IR declarations gets.
// It is either isLocal and then just gets and ordinary number within its module.
// Or is visible across modules and then gets a hash of mangled name as its index.
data class UniqId (
    val index: Long,
    val isLocal: Boolean
)

// isLocal=true in UniqId is good while we dealing with a single current module.
// To disambiguate module local declarations of different modules we use UniqIdKey.
// It has moduleDescriptor specified for isLocal=true uniqIds.
data class UniqIdKey private constructor(val uniqId: UniqId, val moduleDescriptor: ModuleDescriptor?) {
    constructor(moduleDescriptor: ModuleDescriptor?, uniqId: UniqId)
            : this(uniqId, if (uniqId.isLocal) moduleDescriptor!! else null)
}

internal val IrDeclaration.uniqIdIndex: Long
    get() = this.uniqSymbolName().localHash.value

fun protoUniqId(uniqId: UniqId): KonanIr.UniqId =
   KonanIr.UniqId.newBuilder()
       .setIndex(uniqId.index)
       .setIsLocal(uniqId.isLocal)
       .build()

fun KonanIr.UniqId.uniqId(): UniqId = UniqId(this.index, this.isLocal)
fun KonanIr.UniqId.uniqIdKey(moduleDescriptor: ModuleDescriptor) =
    UniqIdKey(moduleDescriptor, this.uniqId())

fun <T, M:GeneratedMessageLite.ExtendableMessage<M>> M.tryGetExtension(extension: GeneratedMessageLite.GeneratedExtension<M, T>)
        = if (this.hasExtension(extension)) this.getExtension<T>(extension) else null

fun DeclarationDescriptor.getUniqId(): KonanProtoBuf.DescriptorUniqId? = when (this) {
    is DeserializedClassDescriptor              -> this.classProto.tryGetExtension(KonanProtoBuf.classUniqId)
    is DeserializedSimpleFunctionDescriptor     -> this.proto.tryGetExtension(KonanProtoBuf.functionUniqId)
    is DeserializedPropertyDescriptor           -> this.proto.tryGetExtension(KonanProtoBuf.propertyUniqId)
    is DeserializedClassConstructorDescriptor   -> this.proto.tryGetExtension(KonanProtoBuf.constructorUniqId)
    else -> null
}

fun newDescriptorUniqId(index: Long): KonanProtoBuf.DescriptorUniqId =
    KonanProtoBuf.DescriptorUniqId.newBuilder().setIndex(index).build()
