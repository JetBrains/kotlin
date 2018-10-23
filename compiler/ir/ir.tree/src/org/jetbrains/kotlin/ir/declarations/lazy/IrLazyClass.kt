/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

class IrLazyClass(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrClassSymbol,
    override val name: Name,
    override val kind: ClassKind,
    override val visibility: Visibility,
    override val modality: Modality,
    override val isCompanion: Boolean,
    override val isInner: Boolean,
    override val isData: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean,
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) :
    IrLazyDeclarationBase(startOffset, endOffset, origin, stubGenerator, typeTranslator),
    IrClass {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrClassSymbol,
        stubGenerator: DeclarationStubGenerator,
        TypeTranslator: TypeTranslator
    ) :
            this(
                startOffset, endOffset, origin, symbol,
                symbol.descriptor.name, symbol.descriptor.kind,
                symbol.descriptor.visibility, symbol.descriptor.modality,
                isCompanion = symbol.descriptor.isCompanionObject,
                isInner = symbol.descriptor.isInner,
                isData = symbol.descriptor.isData,
                isExternal = symbol.descriptor.isEffectivelyExternal(),
                isInline = symbol.descriptor.isInline,
                stubGenerator = stubGenerator,
                typeTranslator = TypeTranslator
            )

    init {
        symbol.bind(this)
    }

    override val annotations: MutableList<IrCall> = arrayListOf()

    override val descriptor: ClassDescriptor get() = symbol.descriptor

    override var thisReceiver: IrValueParameter? by lazyVar {
        typeTranslator.buildWithScope(this) {
            descriptor.thisAsReceiverParameter.generateReceiverParameterStub()
        }
    }


    override val declarations: MutableList<IrDeclaration> by lazyVar {
        ArrayList<IrDeclaration>().also {
            typeTranslator.buildWithScope(this) {
                generateChildStubs(descriptor.constructors, it)
                generateMemberStubs(descriptor.defaultType.memberScope, it)
                generateMemberStubs(descriptor.staticScope, it)
            }
        }.also {
            it.forEach {
                it.parent = this //initialize parent for non lazy cases
            }
        }
    }

    override val typeParameters: MutableList<IrTypeParameter> by lazy {
        descriptor.declaredTypeParameters.mapTo(arrayListOf()) {
            stubGenerator.generateOrGetTypeParameterStub(it)
        }
    }

    override val superTypes: MutableList<IrType> by lazy {
        typeTranslator.buildWithScope(this) {
            // TODO get rid of code duplication, see ClassGenerator#generateClass
            descriptor.typeConstructor.supertypes.mapNotNullTo(arrayListOf()) {
                it.toIrType()
            }
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        thisReceiver?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        thisReceiver = thisReceiver?.transform(transformer, data)
        typeParameters.transform { it.transform(transformer, data) }
        declarations.transform { it.transform(transformer, data) }
    }
}
