package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.serialization.KonanIr
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.KonanLinkData.*
import org.jetbrains.kotlin.serialization.ProtoBuf

fun newUniqId(index: Long): KonanIr.UniqId =
   KonanIr.UniqId.newBuilder().setIndex(index).build() 

// -----------------------------------------------------------

val KonanIr.KotlinDescriptor.index: Long
    get() = this.uniqId.index

fun KonanIr.KotlinDescriptor.Builder.setIndex(index: Long)
    = this.setUniqId(newUniqId(index))

val KonanIr.KotlinDescriptor.originalIndex: Long
    get() = this.originalUniqId.index

fun KonanIr.KotlinDescriptor.Builder.setOriginalIndex(index: Long) 
    = this.setOriginalUniqId(newUniqId(index))

val KonanIr.KotlinDescriptor.dispatchReceiverIndex: Long
    get() = this.dispatchReceiverUniqId.index

fun KonanIr.KotlinDescriptor.Builder.setDispatchReceiverIndex(index: Long) 
    = this.setDispatchReceiverUniqId(newUniqId(index))

val KonanIr.KotlinDescriptor.extensionReceiverIndex: Long
    get() = this.extensionReceiverUniqId.index

fun KonanIr.KotlinDescriptor.Builder.setExtensionReceiverIndex(index: Long) 
    = this.setExtensionReceiverUniqId(newUniqId(index))

// -----------------------------------------------------------

val ProtoBuf.Property.getterIr: InlineIrBody
    get() = this.getExtension(inlineGetterIrBody)

fun ProtoBuf.Property.Builder.setGetterIr(body: InlineIrBody): ProtoBuf.Property.Builder  = 
    this.setExtension(inlineGetterIrBody, body)

val ProtoBuf.Property.setterIr: InlineIrBody
    get() = this.getExtension(inlineSetterIrBody)

fun ProtoBuf.Property.Builder.setSetterIr(body: InlineIrBody): ProtoBuf.Property.Builder = 
    this.setExtension(inlineSetterIrBody, body)

val ProtoBuf.Constructor.constructorIr: InlineIrBody
    get() = this.getExtension(inlineConstructorIrBody)

fun ProtoBuf.Constructor.Builder.setConstructorIr(body: InlineIrBody): ProtoBuf.Constructor.Builder  = 
    this.setExtension(inlineConstructorIrBody, body)

val ProtoBuf.Function.inlineIr: InlineIrBody
    get() = this.getExtension(inlineIrBody)

fun ProtoBuf.Function.Builder.setInlineIr(body: InlineIrBody): ProtoBuf.Function.Builder = 
    this.setExtension(inlineIrBody, body)

// -----------------------------------------------------------

fun inlineBody(encodedIR: String) 
    = KonanLinkData.InlineIrBody
        .newBuilder()
        .setEncodedIr(encodedIR)
        .build()

// -----------------------------------------------------------

internal fun printType(proto: ProtoBuf.Type) {
    println("debug text: " + proto.getExtension(KonanLinkData.typeText))
}

internal fun printTypeTable(proto: ProtoBuf.TypeTable) {
    proto.getTypeList().forEach {
        printType(it)
    }
}

// -----------------------------------------------------------

internal val DeclarationDescriptor.typeParameterProtos: List<ProtoBuf.TypeParameter>
    get() = when (this) {
        // These are different typeParameterLists not 
        // having a common ancestor.
        is DeserializedSimpleFunctionDescriptor
            -> this.proto.typeParameterList
        is DeserializedPropertyDescriptor
            -> this.proto.typeParameterList
        is DeserializedClassDescriptor
            -> this.classProto.typeParameterList
        is DeserializedTypeAliasDescriptor
            -> this.proto.typeParameterList
        is DeserializedClassConstructorDescriptor
            -> listOf()
        else -> error("Unexpected descriptor kind: $this")
    }


