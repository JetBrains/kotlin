package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalClass
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

    override fun serializeType(descriptor: KotlinType, proto: ProtoBuf.Type.Builder) {

        proto.setExtension(KonanLinkData.typeText, descriptor.toString())

        super.serializeType(descriptor, proto)
    }

    override fun serializeTypeParameter(descriptor: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        super.serializeTypeParameter(descriptor, proto)
    }

    override fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {

        val index = inlineDescriptorTable.indexByValue(descriptor)
        proto.setExtension(KonanLinkData.valueParameterIndex, index)

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

        proto.setExtension(KonanLinkData.constructorIndex, 
            inlineDescriptorTable.indexByValue(descriptor))
        val parentIndex = descriptor.parentFqNameIndex()
        proto.setExtension(KonanLinkData.constructorParent, parentIndex)
        
        super.serializeConstructor(descriptor, proto)
    }

    override fun serializeClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder) {

        proto.setExtension(KonanLinkData.classIndex, inlineDescriptorTable.indexByValue(descriptor))
        super.serializeClass(descriptor, proto)
    }

    override fun serializeFunction(descriptor: FunctionDescriptor, proto: ProtoBuf.Function.Builder) {

        proto.setExtension(KonanLinkData.functionIndex, 
            inlineDescriptorTable.indexByValue(descriptor))
        val parentIndex = descriptor.parentFqNameIndex()
        proto.setExtension(KonanLinkData.functionParent, parentIndex)
  
        if (descriptor.isInline()) {

            val encodedIR: String = IrSerializer( context, 
                inlineDescriptorTable, stringTable, util, descriptor)
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


    override fun serializeProperty(property: PropertyDescriptor, proto: ProtoBuf.Property.Builder) {
        val parentIndex = property.parentFqNameIndex()
        proto.setExtension(KonanLinkData.propertyParent, parentIndex)
        val variable = originalVariables[property]
        if (variable != null) {
            proto.setExtension(KonanLinkData.usedAsVariable, true)
            proto.setExtension(KonanLinkData.propertyIndex, inlineDescriptorTable.indexByValue(variable))

        } else {
            proto.setExtension(KonanLinkData.propertyIndex, inlineDescriptorTable.indexByValue(property))
        }

        super.serializeProperty(property, proto)

        if (context.ir.propertiesWithBackingFields.contains(property)) {
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
