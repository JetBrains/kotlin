/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.DescriptorUniqIdAware
import org.jetbrains.kotlin.backend.common.serialization.tryGetExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*

class JvmDescriptorUniqIdAware(val symbolTable: SymbolTable, val stubGenerator: DeclarationStubGenerator) : DescriptorUniqIdAware {
    override fun DeclarationDescriptor.getUniqId(): Long? =
        when (this) {
            is DeserializedClassDescriptor -> this.classProto.tryGetExtension(KlibMetadataProtoBuf.classUniqId)?.index
                ?: referenceAndHash(this)
            is DeserializedSimpleFunctionDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.functionUniqId)?.index
                ?: referenceAndHash(this)
            is DeserializedPropertyDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.propertyUniqId)?.index
                ?: referenceAndHash(this)
            is DeserializedClassConstructorDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.constructorUniqId)?.index
                ?: referenceAndHash(this)
            is DeserializedTypeParameterDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.typeParamUniqId)?.index
                ?: referenceAndHash(this)
            is DeserializedTypeAliasDescriptor -> this.proto.tryGetExtension(KlibMetadataProtoBuf.typeAliasUniqId)?.index
                ?: referenceAndHash(this)
            else -> referenceAndHash(this)
        }

    private fun referenceAndHash(descriptor: DeclarationDescriptor): Long? =
        if (descriptor is CallableMemberDescriptor && descriptor.kind === CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
            null
        else with(JvmMangler) {
            referenceWithParents(descriptor).hashedMangle
        }


    private fun referenceWithParents(descriptor: DeclarationDescriptor): IrDeclaration {
        val original = descriptor.original
        val result = referenceOrDeclare(original)
        var currentDescriptor = original
        var current = result
        // If current is a lazy declaration, the parent may already be set.
        while (current.parent == null) {
            val nextDescriptor = when {
                currentDescriptor is TypeParameterDescriptor && currentDescriptor.containingDeclaration is PropertyDescriptor -> {
                    val property = currentDescriptor.containingDeclaration as PropertyDescriptor
                    // No way to choose between getter and setter by descriptor alone :(
                    property.getter ?: property.setter!!
                }
                else ->
                    currentDescriptor.containingDeclaration!!
            }
            if (nextDescriptor is PackageFragmentDescriptor) {
                current.parent = symbolTable.findOrDeclareExternalPackageFragment(nextDescriptor)
                break
            } else {
                val next = referenceOrDeclare(nextDescriptor)
                current.parent = next as IrDeclarationParent
                currentDescriptor = nextDescriptor
                current = next
            }
        }
        return result
    }

    private fun referenceOrDeclare(descriptor: DeclarationDescriptor): IrDeclaration =
        symbolTable.referenceMember(descriptor).also {
            if (!it.isBound) {
                stubGenerator.getDeclaration(it)
            }
        }.owner as IrDeclaration
}

// May be needed in the future
//
//fun DeclarationDescriptor.willBeEliminatedInLowerings(): Boolean =
//        isAnnotationConstructor() ||
//                (this is PropertyAccessorDescriptor &&
//                        correspondingProperty.hasJvmFieldAnnotation())
