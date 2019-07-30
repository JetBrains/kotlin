/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.proto.DescriptorReference as ProtoDescriptorReference
import org.jetbrains.kotlin.backend.common.serialization.proto.String as ProtoString

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.isAccessor
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.isSetter
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.classId


open class DescriptorReferenceSerializer(
    val declarationTable: DeclarationTable,
    val serializeString: (String) -> ProtoString,
    mangler: KotlinMangler): KotlinMangler by mangler {

    private fun isEnumSpecialMember(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor !is SimpleFunctionDescriptor) return false

        return DescriptorFactory.isEnumValueOfMethod(descriptor) || DescriptorFactory.isEnumValuesMethod(descriptor)
    }

    fun extractPackageAndClassFqns(descriptor: DeclarationDescriptor): Pair<String, String>? {
        val containingDeclaration = descriptor.containingDeclaration
        return when (containingDeclaration) {
            is ClassDescriptor -> {
                val classId = containingDeclaration.classId ?: return null
                Pair(classId.packageFqName.toString(), classId.relativeClassName.toString())
            }
            is PackageFragmentDescriptor -> Pair(containingDeclaration.fqName.toString(), "")
            is PropertyDescriptor -> if (descriptor !is TypeParameterDescriptor) null else {
                extractPackageAndClassFqns(containingDeclaration)
            }
            else -> return null
        }
    }

    // Not all exported descriptors are deserialized, some a synthesized anew during metadata deserialization.
    // Those created descriptors can't carry the uniqIdIndex, since it is available only for deserialized descriptors.
    // So we record the uniq id of some other "discoverable" descriptor for which we know for sure that it will be
    // available as deserialized descriptor, plus the path to find the needed descriptor from that one.
    fun serializeDescriptorReference(declaration: IrDeclaration): ProtoDescriptorReference? {

        val descriptor = declaration.descriptor

        if (!declaration.isExported() &&
            !((declaration as? IrDeclarationWithVisibility)?.visibility == Visibilities.INVISIBLE_FAKE)) {
            return null
        }
        if (declaration is IrAnonymousInitializer) return null

        if (descriptor is ParameterDescriptor || (descriptor is VariableDescriptor && descriptor !is PropertyDescriptor)) return null

        val containingDeclaration = descriptor.containingDeclaration!!
        val (packageFqName, classFqName) = extractPackageAndClassFqns(descriptor) ?: return null

        val isAccessor = declaration.isAccessor
        val isBackingField = declaration is IrField && declaration.correspondingProperty != null
        val isFakeOverride = declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE
        val isDefaultConstructor = descriptor is ClassConstructorDescriptor && containingDeclaration is ClassDescriptor && (containingDeclaration.kind == ClassKind.OBJECT)
        val isEnumEntry = descriptor is ClassDescriptor && descriptor.kind == ClassKind.ENUM_ENTRY
        val isEnumSpecial = isEnumSpecialMember(descriptor)
        val isTypeParameter = declaration is IrTypeParameter

        // The corresponding descriptor in deserialized metadata has constructors = emptyList() etc.
        if (containingDeclaration is ClassDescriptor &&
            containingDeclaration.kind == ClassKind.ENUM_ENTRY &&
            !isFakeOverride) return null

        val realDeclaration = if (isFakeOverride) {
            when (declaration) {
                is IrSimpleFunction -> declaration.resolveFakeOverrideMaybeAbstract()
                is IrField -> declaration.resolveFakeOverride()
                is IrProperty -> declaration.resolveFakeOverrideMaybeAbstract()
                else -> error("Unexpected fake override declaration")
            }
        } else {
            declaration
        }

        val discoverableDescriptorsDeclaration: IrDeclaration? = if (isAccessor) {
            (realDeclaration as IrSimpleFunction).correspondingProperty!!
        } else if (isBackingField) {
            (realDeclaration as IrField).correspondingProperty!!
        } else if (isDefaultConstructor || isEnumEntry) {
            null
        } else {
            realDeclaration
        }

        val nameString = if (isTypeParameter) {
            val parent = declaration.parent
            val typeParameterContainer = when (parent) {
                is IrClass -> parent
                is IrSimpleFunction -> parent.correspondingPropertySymbol?.owner ?: parent
                is IrConstructor -> parent
                else -> error("unknown type parameter container type")
            }
            typeParameterContainer.descriptor.name.asString()
        } else descriptor.name.toString()

        val uniqId = discoverableDescriptorsDeclaration?.let { declarationTable.uniqIdByDeclaration(it) }
        uniqId?.let { declarationTable.descriptors.put(discoverableDescriptorsDeclaration.descriptor, it) }

        val proto = ProtoDescriptorReference.newBuilder()
            .setPackageFqName(serializeString(packageFqName))
            .setClassFqName(serializeString(classFqName))
            .setName(serializeString(nameString))

        if (uniqId != null) proto.setUniqId(protoUniqId(uniqId))

        if (isFakeOverride) {
            proto.setIsFakeOverride(true)
        }

        if (isBackingField) {
            proto.setIsBackingField(true)
        }

        if (isAccessor) {
            if (declaration.isGetter)
                proto.setIsGetter(true)
            else if (declaration.isSetter)
                proto.setIsSetter(true)
            else
                error("A property accessor which is neither a getter, nor a setter: $descriptor")
        } else if (isDefaultConstructor) {
            proto.setIsDefaultConstructor(true)
        } else if (isEnumEntry) {
            proto.setIsEnumEntry(true)
        } else if (isEnumSpecial) {
            proto.setIsEnumSpecial(true)
        } else if (isTypeParameter) {
            proto.setIsTypeParameter(true)
        }

        return proto.build()
    }
}


