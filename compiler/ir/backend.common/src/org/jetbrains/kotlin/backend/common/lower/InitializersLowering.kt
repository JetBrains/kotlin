/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.deepCopyWithWrappedDescriptors
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.SetDeclarationsParentVisitor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DescriptorsRemapper
import org.jetbrains.kotlin.ir.util.SymbolRenamer
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns

object SYNTHESIZED_INIT_BLOCK: IrStatementOriginImpl("SYNTHESIZED_INIT_BLOCK")

class InitializersLowering(
    val context: CommonBackendContext,
    val declarationOrigin: IrDeclarationOrigin,
    private val clinitNeeded: Boolean
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val instanceInitializerStatements = handleNonStatics(irClass)
        transformInstanceInitializerCallsInConstructors(irClass, instanceInitializerStatements)

        val staticInitializerStatements = handleStatics(irClass)
        if (clinitNeeded && staticInitializerStatements.isNotEmpty())
            createStaticInitializationMethod(irClass, staticInitializerStatements)
        irClass.patchDeclarationParents(irClass.parent)
    }

    fun handleNonStatics(irClass: IrClass) =
        irClass.declarations.filter {
            (it is IrField && !it.isStatic) || (it is IrAnonymousInitializer && !it.isStatic)
        }.mapNotNull { handleDeclaration(irClass, it) }

    fun handleStatics(irClass: IrClass) =
    // Hardcoded order of initializers
        (irClass.declarations.filter { it is IrField && it.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY } +
                irClass.declarations.filter { it is IrField && it.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_VALUES } +
                irClass.declarations.filter { it is IrField && it.origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE } +
                irClass.declarations.filter {
                    (it is IrField && it.isStatic && it.origin !in listOf(
                        IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY,
                        IrDeclarationOrigin.FIELD_FOR_ENUM_VALUES,
                        IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
                    )) || (it is IrAnonymousInitializer && it.isStatic)
                })
            .mapNotNull { handleDeclaration(irClass, it) }

    fun handleDeclaration(irClass: IrClass, declaration: IrDeclaration): IrStatement? = when(declaration) {
        is IrField -> handleField(irClass, declaration)
        is IrAnonymousInitializer -> handleAnonymousInitializer(declaration)
        else -> null
    }

    fun handleField(irClass: IrClass, declaration: IrField): IrStatement? {
        val irFieldInitializer = declaration.initializer?.expression ?: return null

        val receiver =
            if (!declaration.isStatic) // TODO isStaticField
                IrGetValueImpl(
                    irFieldInitializer.startOffset, irFieldInitializer.endOffset,
                    irClass.thisReceiver!!.type, irClass.thisReceiver!!.symbol
                )
            else null
        return IrSetFieldImpl(
            irFieldInitializer.startOffset, irFieldInitializer.endOffset,
            declaration.symbol,
            receiver,
            irFieldInitializer,
            context.irBuiltIns.unitType,
            null, null
        )
    }


    fun handleAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement = IrBlockImpl(
        declaration.startOffset, declaration.endOffset,
        context.irBuiltIns.unitType,
        SYNTHESIZED_INIT_BLOCK,
        declaration.body.statements
    )

    fun transformInstanceInitializerCallsInConstructors(irClass: IrClass, instanceInitializerStatements: List<IrStatement>) {
        irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression {
                val copiedBlock =
                    IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, null, instanceInitializerStatements).copy(
                        irClass
                    ) as IrBlock
                return IrBlockImpl(irClass.startOffset, irClass.endOffset, context.irBuiltIns.unitType, null, copiedBlock.statements)
            }
        })
    }

    fun createStaticInitializationMethod(irClass: IrClass, staticInitializerStatements: List<IrStatement>) {
        val staticInitializerDescriptor = SimpleFunctionDescriptorImpl.create(
            irClass.descriptor, Annotations.EMPTY, clinitName,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        )
        staticInitializerDescriptor.initialize(
            null, null, emptyList(), emptyList(),
            irClass.descriptor.builtIns.unitType,
            Modality.FINAL, Visibilities.PUBLIC
        )
        irClass.declarations.add(
            IrFunctionImpl(
                irClass.startOffset, irClass.endOffset, declarationOrigin,
                staticInitializerDescriptor,
                context.irBuiltIns.unitType,
                IrBlockBodyImpl(irClass.startOffset, irClass.endOffset,
                                staticInitializerStatements.map { it.copy(irClass) })
            ).apply {
                accept(SetDeclarationsParentVisitor, this)
            }
        )
    }

    companion object {
        val clinitName = Name.special("<clinit>")

        fun IrStatement.copy(containingDeclaration: IrDeclarationParent) = deepCopyWithWrappedDescriptors(containingDeclaration)
        fun IrExpression.copy(containingDeclaration: IrDeclarationParent) = deepCopyWithWrappedDescriptors(containingDeclaration)
    }
}
