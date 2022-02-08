/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators.fragments

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorPublicSymbolImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockCodeFragment
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi2ir.generators.Generator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.createBodyGenerator
import org.jetbrains.kotlin.resolve.BindingContextUtils
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
        constructor.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
            statements +=
                delegatingCallToAnyConstructor()
        }
        irClass.addMember(constructor)
    }

    private fun delegatingCallToAnyConstructor(): IrStatement {
        val anyConstructor = context.irBuiltIns.anyClass.descriptor.constructors.single()
        return IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.unitType,
            context.symbolTable.referenceConstructor(anyConstructor)
        )
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
            val parameterInfo = fragmentInfo.parameters[index]
            irFunction.valueParameters += declareParameter(valueParameterDescriptor, parameterInfo).apply {
                context.fragmentContext!!.capturedDescriptorToFragmentParameterMap[parameterInfo.descriptor] = this.symbol
            }
        }
    }

    private fun declareParameter(descriptor: ValueParameterDescriptor, parameterInfo: EvaluatorFragmentParameterInfo): IrValueParameter {
        // Parameter must be _assignable_ if written by the fragment:
        // These parameters model the captured variables of the fragment. The
        // captured _values_ are extracted from the call stack of the JVM being
        // debugged, and supplied to the fragment evaluator via these
        // parameters.
        //
        // If the parameter is modified by the fragment, the parameter is boxed
        // in a `Ref`, and the value extracted from that box upon return from
        // the fragment, then written back into the stack frame of the JVM
        // being debugged.
        //
        // The promotion to `Ref` and the replacement of loads/stores with the
        // appropriate getfield/putfield API of the `Ref` is done _after_
        // psi2ir, so for now we must mark the parameter as assignable for the
        // of IR generation. The replacement is delayed because the JVM
        // specific infrastructure (i.e. "SharedVariableContext") is not yet
        // instantiated: PSI2IR is kept backend agnostic.
        return context.symbolTable.declareValueParameter(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            if (shouldPromoteToSharedVariable(parameterInfo)) IrDeclarationOrigin.SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT else IrDeclarationOrigin.DEFINED,
            descriptor,
            descriptor.type.toIrType(),
            descriptor.varargElementType?.toIrType(),
            null,
            isAssignable = parameterInfo.isLValue
        )
    }

    private fun shouldPromoteToSharedVariable(parameterInfo: EvaluatorFragmentParameterInfo) =
        parameterInfo.isLValue ||
                BindingContextUtils.isBoxedLocalCapturedInClosure(
                    context.bindingContext,
                    parameterInfo.descriptor
                )


    private fun KotlinType.toIrType() = context.typeTranslator.translateType(this)

    private inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration: T ->
            context.symbolTable.withScope(irDeclaration) {
                builder(irDeclaration)
            }
        }

}