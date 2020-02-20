/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.util.mapOptimized
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class IrLazyClass(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrClassSymbol,
    override val descriptor: ClassDescriptor,
    override val name: Name,
    override val kind: ClassKind,
    override var visibility: Visibility,
    override var modality: Modality,
    override val isCompanion: Boolean,
    override val isInner: Boolean,
    override val isData: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean,
    override val isExpect: Boolean,
    override val isFun: Boolean,
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) :
    IrLazyDeclarationBase(startOffset, endOffset, origin, stubGenerator, typeTranslator),
    IrClass {

    init {
        symbol.bind(this)
    }

    override var thisReceiver: IrValueParameter? by lazyVar {
        typeTranslator.buildWithScope(this) {
            descriptor.thisAsReceiverParameter.generateReceiverParameterStub().apply { parent = this@IrLazyClass }
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

    override var typeParameters: List<IrTypeParameter> by lazyVar {
        descriptor.declaredTypeParameters.mapTo(arrayListOf()) {
            stubGenerator.generateOrGetTypeParameterStub(it)
        }
    }

    override var superTypes: List<IrType> by lazyVar {
        typeTranslator.buildWithScope(this) {
            // TODO get rid of code duplication, see ClassGenerator#generateClass
            descriptor.typeConstructor.supertypes.mapNotNullTo(arrayListOf()) {
                it.toIrType()
            }
        }
    }

    override var attributeOwnerId: IrAttributeContainer = this

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        thisReceiver?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        thisReceiver = thisReceiver?.transform(transformer, data)
        typeParameters = typeParameters.mapOptimized { it.transform(transformer, data) }
        declarations.transform { it.transform(transformer, data) }
    }
}
