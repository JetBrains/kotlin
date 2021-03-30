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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

class IrLazyClass(
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    override val symbol: IrClassSymbol,
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: ClassDescriptor,
    override val name: Name,
    override val kind: ClassKind,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    override val isCompanion: Boolean,
    override val isInner: Boolean,
    override val isData: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean,
    override val isExpect: Boolean,
    override val isFun: Boolean,
    override val stubGenerator: DeclarationStubGenerator,
    override val typeTranslator: TypeTranslator
) : IrClass(), IrLazyDeclarationBase {
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
        }.also {
            it.forEach {
                it.parent = this //initialize parent for non lazy cases
            }
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
                // Always build lazy IR stubs for inline class primary constructors.
                // This is needed because primary constructors of inline classes can be private, and prior to 1.5.0, there was no way
                // to determine the inline class representation other than by loading the single value parameter of the primary constructor
                // (corresponding metadata entry was added in 1.5.0). Backend still tries to load inline class representation in this way
                // to support compilation against inline classes compiled with 1.4.30 or earlier.
                (descriptor is ConstructorDescriptor && isInline)

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

    override var attributeOwnerId: IrAttributeContainer = this

    val classProto: ProtoBuf.Class? get() = (descriptor as? DeserializedClassDescriptor)?.classProto
    val nameResolver: NameResolver? get() = (descriptor as? DeserializedClassDescriptor)?.c?.nameResolver
    override val source: SourceElement get() = descriptor.source

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")
}
