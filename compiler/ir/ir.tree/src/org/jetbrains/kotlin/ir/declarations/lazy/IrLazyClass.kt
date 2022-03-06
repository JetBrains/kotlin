/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.SimpleType

class IrLazyClass(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrClassSymbol,
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: ClassDescriptor,
    override var name: Name,
    override val kind: ClassKind,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    override val isCompanion: Boolean,
    override val isInner: Boolean,
    override val isData: Boolean,
    override val isExternal: Boolean,
    override val isValue: Boolean,
    override val isExpect: Boolean,
    override val isFun: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator
) : IrClass(), IrLazyDeclarationBase, DeserializableClass {
    init {
        symbol.bind(this)
    }

    override var parent: IrDeclarationParent by createLazyParent()

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override var thisReceiver: IrValueParameter? by lazyVar(stubGenerator.lock) {
        typeTranslator.buildWithScope(this) {
            descriptor.thisAsReceiverParameter.generateReceiverParameterStub().apply { parent = this@IrLazyClass }
        }
    }


    override val declarations: MutableList<IrDeclaration> by lazyVar(stubGenerator.lock) {
        ArrayList<IrDeclaration>().also {
            typeTranslator.buildWithScope(this) {
                generateChildStubs(descriptor.constructors, it)
                generateChildStubs(descriptor.defaultType.memberScope.getContributedDescriptors(), it)
                generateChildStubs(descriptor.staticScope.getContributedDescriptors(), it)
            }
        }.onEach {
            it.parent = this //initialize parent for non lazy cases
        }
    }

    private fun generateChildStubs(descriptors: Collection<DeclarationDescriptor>, declarations: MutableList<IrDeclaration>) {
        descriptors.mapNotNullTo(declarations) { descriptor ->
            if (shouldBuildStub(descriptor)) stubGenerator.generateMemberStub(descriptor) else null
        }
    }

    private fun shouldBuildStub(descriptor: DeclarationDescriptor): Boolean =
        descriptor !is DeclarationDescriptorWithVisibility ||
                !DescriptorVisibilities.isPrivate(descriptor.visibility) ||
                // This exception is needed for K/N caches usage.
                isObject && descriptor is ClassConstructorDescriptor

    override var typeParameters: List<IrTypeParameter> by lazyVar(stubGenerator.lock) {
        descriptor.declaredTypeParameters.mapTo(arrayListOf()) {
            stubGenerator.generateOrGetTypeParameterStub(it)
        }
    }

    override var superTypes: List<IrType> by lazyVar(stubGenerator.lock) {
        typeTranslator.buildWithScope(this) {
            // TODO get rid of code duplication, see ClassGenerator#generateClass
            descriptor.typeConstructor.supertypes.mapNotNullTo(arrayListOf()) {
                it.toIrType()
            }
        }
    }

    override var sealedSubclasses: List<IrClassSymbol> by lazyVar(stubGenerator.lock) {
        descriptor.sealedSubclasses.map { sealedSubclassDescriptor ->
            // NB 'generateClassStub' would return an existing class if it's already present in symbol table
            stubGenerator.generateClassStub(sealedSubclassDescriptor).symbol
        }
    }

    override var inlineClassRepresentation: InlineClassRepresentation<IrSimpleType>? by lazyVar(stubGenerator.lock) {
        descriptor.inlineClassRepresentation?.mapUnderlyingType(::simplyTypeToIrOrThrow)
    }

    private fun simplyTypeToIrOrThrow(it: SimpleType) =
        it.toIrType() as? IrSimpleType ?: error("Inline class underlying type is not a simple type: ${render()}")

    override var multiFieldValueClassRepresentation: MultiFieldValueClassRepresentation<IrSimpleType>? by lazyVar(stubGenerator.lock) {
        descriptor.multiFieldValueClassRepresentation?.mapUnderlyingType(::simplyTypeToIrOrThrow)
    }

    override var attributeOwnerId: IrAttributeContainer = this

    val classProto: ProtoBuf.Class? get() = (descriptor as? DeserializedClassDescriptor)?.classProto
    val nameResolver: NameResolver? get() = (descriptor as? DeserializedClassDescriptor)?.c?.nameResolver
    override val source: SourceElement get() = descriptor.source

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    private var irLoaded: Boolean? = null

    override fun loadIr(): Boolean {
        assert(parent is IrPackageFragment)
        return irLoaded ?:
            stubGenerator.extensions.deserializeClass(this, stubGenerator, parent, allowErrorNodes = false).also { irLoaded = it }
    }
}
