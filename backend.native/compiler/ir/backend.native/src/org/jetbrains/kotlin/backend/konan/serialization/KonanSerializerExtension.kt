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

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalClass
import org.jetbrains.kotlin.backend.konan.descriptors.needsInlining
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.*
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.KonanLinkData.InlineIrBody
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType


internal class KonanSerializerExtension(val context: Context, val util: KonanSerializationUtil) :
        KotlinSerializerExtensionBase(KonanSerializerProtocol) {

    val inlineDescriptorTable = DescriptorTable(context.irBuiltIns)
    val originalVariables = mutableMapOf<PropertyDescriptor, VariableDescriptor>()
    override val stringTable = KonanStringTable()
    override fun shouldUseTypeTable(): Boolean = true

    override fun serializeType(type: KotlinType, proto: ProtoBuf.Type.Builder) {

        proto.setExtension(KonanLinkData.typeText, type.toString())

        super.serializeType(type, proto)
    }

    override fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        super.serializeTypeParameter(typeParameter, proto)
    }

    override fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
        super.serializeValueParameter(descriptor, proto)
    }

    override fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {

        super.serializeEnumEntry(descriptor, proto)
    }

    fun DeclarationDescriptor.parentFqNameIndex(): Int {

        if (this.containingDeclaration is ClassOrPackageFragmentDescriptor) {
            val parentIndex = stringTable.getClassOrPackageFqNameIndex(
                    this.containingDeclaration as ClassOrPackageFragmentDescriptor)
            return parentIndex
        } else {
            return -1
        }
    }

    override fun serializeConstructor(descriptor: ConstructorDescriptor, proto: ProtoBuf.Constructor.Builder) {

        proto.setConstructorIndex(
            inlineDescriptorTable.indexByValue(descriptor))
        val parentIndex = descriptor.parentFqNameIndex()
        proto.setExtension(KonanLinkData.constructorParent, parentIndex)
        
        super.serializeConstructor(descriptor, proto)
    }

    override fun serializeClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder) {

        proto.setClassIndex(
            inlineDescriptorTable.indexByValue(descriptor))
        super.serializeClass(descriptor, proto)
    }

    override fun serializeFunction(descriptor: FunctionDescriptor, proto: ProtoBuf.Function.Builder) = serializeFunctionWithIR(descriptor, proto, null)

    fun serializeFunctionWithIR(descriptor: FunctionDescriptor, proto: ProtoBuf.Function.Builder, typeSerializer: ((KotlinType)->Int)?) {

        proto.setFunctionIndex(
            inlineDescriptorTable.indexByValue(descriptor))
        val parentIndex = descriptor.parentFqNameIndex()
        proto.setExtension(KonanLinkData.functionParent, parentIndex)
  
        if (descriptor.needsInlining) {
            val encodedIR: String = IrSerializer( context, 
                inlineDescriptorTable, stringTable, util, typeSerializer!!, descriptor)
                    .serializeInlineBody()

            val inlineIrBody = KonanLinkData.InlineIrBody
                .newBuilder()
                .setEncodedIr(encodedIR)
                .build()

            proto.setExtension(KonanLinkData.inlineIrBody, inlineIrBody)

        }
        super.serializeFunction(descriptor, proto)
    }

    private val backingFieldClass = 
        context.builtIns.getKonanInternalClass("HasBackingField").getDefaultType()

    private val backingFieldAnnotation = AnnotationDescriptorImpl(
       backingFieldClass, emptyMap(), SourceElement.NO_SOURCE)


    override fun serializeProperty(descriptor: PropertyDescriptor, proto: ProtoBuf.Property.Builder) {
        val parentIndex = descriptor.parentFqNameIndex()
        proto.setExtension(KonanLinkData.propertyParent, parentIndex)
        val variable = originalVariables[descriptor]
        if (variable != null) {
            proto.setExtension(KonanLinkData.usedAsVariable, true)
            proto.setPropertyIndex(
                inlineDescriptorTable.indexByValue(variable))

        } else {
            proto.setPropertyIndex(
                inlineDescriptorTable.indexByValue(descriptor))
        }

        super.serializeProperty(descriptor, proto)

        if (context.ir.propertiesWithBackingFields.contains(descriptor)) {
            proto.addExtension(KonanLinkData.propertyAnnotation, 
                annotationSerializer.serializeAnnotation(backingFieldAnnotation))

            proto.flags = proto.flags or Flags.HAS_ANNOTATIONS.toFlags(true)
        }
    }
}

object KonanSerializerProtocol : SerializerExtensionProtocol(
        ExtensionRegistryLite.newInstance().apply {
           KonanLinkData.registerAllExtensions(this)
        },
        KonanLinkData.packageFqName,
        KonanLinkData.constructorAnnotation,
        KonanLinkData.classAnnotation,
        KonanLinkData.functionAnnotation,
        KonanLinkData.propertyAnnotation,
        KonanLinkData.enumEntryAnnotation,
        KonanLinkData.compileTimeValue,
        KonanLinkData.parameterAnnotation,
        KonanLinkData.typeAnnotation,
        KonanLinkData.typeParameterAnnotation
)
