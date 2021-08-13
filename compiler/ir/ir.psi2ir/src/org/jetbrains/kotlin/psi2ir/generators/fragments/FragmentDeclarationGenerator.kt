/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators.fragments

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorPublicSymbolImpl
import org.jetbrains.kotlin.ir.util.createIrClassFromDescriptor
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockCodeFragment
import org.jetbrains.kotlin.psi2ir.generators.Generator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.createBodyGenerator
import org.jetbrains.kotlin.types.KotlinType

open class FragmentDeclarationGenerator(
    override val context: GeneratorContext,
    private val fragmentInfo: EvaluatorFragmentInfo
) : Generator {

    fun generateClassForCodeFragment(ktFile: KtBlockCodeFragment): IrClass {
        val classDescriptor = fragmentInfo.classDescriptor
        val startOffset = UNDEFINED_OFFSET
        val endOffset = UNDEFINED_OFFSET

        return context.symbolTable.declareClass(classDescriptor) {
            context.irFactory.createIrClassFromDescriptor(
                startOffset, endOffset,
                IrDeclarationOrigin.DEFINED,
                symbol = it,
                classDescriptor,
                context.symbolTable.nameProvider.nameForDeclaration(classDescriptor),
                classDescriptor.visibility,
                Modality.FINAL
            )
        }.buildWithScope { irClass ->
            irClass.thisReceiver = context.symbolTable.declareValueParameter(
                startOffset, endOffset,
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                classDescriptor.thisAsReceiverParameter,
                classDescriptor.thisAsReceiverParameter.type.toIrType()
            )

            generatePrimaryConstructor(irClass)

            irClass.declarations.add(
                generateFunctionForFragment(ktFile)
            )
        }
    }

    private fun generatePrimaryConstructor(irClass: IrClass) {
        val constructor = context.irFactory.createConstructor(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            symbol = IrConstructorPublicSymbolImpl(context.symbolTable.signaturer.composeSignature(irClass.descriptor)!!),
            Name.special("<init>"),
            irClass.visibility,
            irClass.defaultType,
            isInline = false,
            isExternal = false,
            isPrimary = true,
            isExpect = false
        )
        constructor.parent = irClass
        constructor.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        irClass.addMember(constructor)
    }

    private fun generateFunctionForFragment(ktFile: KtBlockCodeFragment): IrSimpleFunction {
        return context.symbolTable.declareSimpleFunctionWithOverrides(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            fragmentInfo.methodDescriptor
        ).buildWithScope { irFunction ->
            irFunction.returnType = fragmentInfo.methodDescriptor.returnType!!.toIrType()
            generateFragmentValueParameterDeclarations(irFunction)
            irFunction.body = createBodyGenerator(irFunction.symbol).generateExpressionBody(ktFile.getContentElement())
        }
    }

    private fun generateFragmentValueParameterDeclarations(irFunction: IrSimpleFunction) {
        val functionDescriptor = irFunction.descriptor
        functionDescriptor.valueParameters.forEachIndexed { index, valueParameterDescriptor ->
            irFunction.valueParameters += declareParameter(valueParameterDescriptor).apply {
                context.fragmentContext!!.capturedDescriptorToFragmentParameterMap[fragmentInfo.parameters[index]] = this.symbol
            }
        }
    }

    private fun declareParameter(descriptor: ValueParameterDescriptor): IrValueParameter {
        // Parameter must be _assignable_:
        // These parameters model the captured variables of the fragment. The captured
        // _values_ are extracted from the call stack of the JVM being debugged, and supplied
        // to the fragment evaluator via these parameters. Any modifications by the fragment
        // are written directly to the parameter, and then extracted from the stack frame
        // of the interpreter/JVM evaluating the fragment and written back into the call
        // stack of the JVM being debugged.
        return context.symbolTable.declareValueParameter(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            descriptor,
            descriptor.type.toIrType(),
            (descriptor as? ValueParameterDescriptor)?.varargElementType?.toIrType(),
            null,
            isAssignable = true
        )
    }

    private fun KotlinType.toIrType() = context.typeTranslator.translateType(this)

    private inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration: T ->
            context.symbolTable.withScope(irDeclaration) {
                builder(irDeclaration)
            }
        }

}