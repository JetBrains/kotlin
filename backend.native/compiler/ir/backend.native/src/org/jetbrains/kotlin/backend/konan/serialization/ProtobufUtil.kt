package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.serialization.KonanIr
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.KonanLinkData.*
import org.jetbrains.kotlin.serialization.ProtoBuf

fun newUniqId(index: Long): KonanLinkData.UniqId =
   KonanLinkData.UniqId.newBuilder().setIndex(index).build() 

val ProtoBuf.Function.functionIndex: Long
    get() = this.getExtension(KonanLinkData.functionIndex).index

val ProtoBuf.Constructor.constructorIndex: Long
    get() = this.getExtension(KonanLinkData.constructorIndex).index

val ProtoBuf.Property.propertyIndex: Long
    get() = this.getExtension(KonanLinkData.propertyIndex).index


fun ProtoBuf.Function.Builder.setFunctionIndex(index: Long)
    = this.setExtension(KonanLinkData.functionIndex, newUniqId(index))

fun ProtoBuf.Constructor.Builder.setConstructorIndex(index: Long)
    = this.setExtension(KonanLinkData.constructorIndex, newUniqId(index))

fun ProtoBuf.Property.Builder.setPropertyIndex(index: Long)
    = this.setExtension(KonanLinkData.propertyIndex, newUniqId(index))

fun ProtoBuf.Class.Builder.setClassIndex(index: Long)
    = this.setExtension(KonanLinkData.classIndex, newUniqId(index))

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


