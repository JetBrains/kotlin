/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.lazy

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.declarations.lazy.IrMaybeDeserializedClass
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.deserializedIr
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

class IrLazyClass(
    override var startOffset: Int,
    override var endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrClassSymbol,
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: ClassDescriptor,
    override var name: Name,
    override var kind: ClassKind,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    override var isCompanion: Boolean,
    override var isInner: Boolean,
    override var isData: Boolean,
    override var isExternal: Boolean,
    override var isValue: Boolean,
    override var isExpect: Boolean,
    override var isFun: Boolean,
    override var hasEnumEntries: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator,
) : IrClass(), IrLazyDeclarationBase, IrMaybeDeserializedClass {
    init {
        symbol.bind(this)
        this.deserializedIr = lazy {
            assert(parent is IrPackageFragment)
            stubGenerator.extensions.deserializeClass(this, stubGenerator, parent)
        }
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override var thisReceiver: IrValueParameter? by lazyVar(stubGenerator.lock) {
        typeTranslator.buildWithScope(this) {
            descriptor.thisAsReceiverParameter.generateReceiverParameterStub().apply { parent = this@IrLazyClass }
        }
    }

    @UnsafeDuringIrConstructionAPI
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
        descriptor !is DeclarationDescriptorWithVisibility
                || (!DescriptorVisibilities.isPrivate(descriptor.visibility) && descriptor.visibility != DescriptorVisibilities.INVISIBLE_FAKE)
                // This exception is needed for K/N caches usage.
                || isObject && descriptor is ClassConstructorDescriptor

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

    override var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>? by lazyVar(stubGenerator.lock) {
        descriptor.valueClassRepresentation?.mapUnderlyingType {
            it.toIrType() as? IrSimpleType ?: error("Value class underlying type is not a simple type: ${render()}")
        }
    }

    override var attributeOwnerId: IrElement = this

    val classProto: ProtoBuf.Class? get() = (descriptor as? DeserializedClassDescriptor)?.classProto
    val nameResolver: NameResolver? get() = (descriptor as? DeserializedClassDescriptor)?.c?.nameResolver
    override val source: SourceElement get() = descriptor.source

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override val moduleName: String?
        get() = classProto?.getExtensionOrNull(JvmProtoBuf.classModuleName)?.let { nameResolver?.getString(it) }

    override val isNewPlaceForBodyGeneration: Boolean?
        get() = classProto?.let { JvmProtoBufUtil.isNewPlaceForBodyGeneration(it) }

    override val isK2: Boolean = false
}