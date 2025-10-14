/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.functionReferenceLinkageError
import org.jetbrains.kotlin.backend.common.functionReferenceReflectedName
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSources
import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.invokeFun
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isKSuspendFunction
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import kotlin.collections.plus

abstract class WebCallableReferenceLowering(context: CommonBackendContext) :
    AbstractFunctionReferenceLowering<CommonBackendContext>(context) {

    protected val IrRichFunctionReference.isLambda: Boolean
        get() = origin.isLambda

    protected val IrRichFunctionReference.isKReference: Boolean
        get() = type.let { it.isKFunction() || it.isKSuspendFunction() }

    private val nothingType = context.irBuiltIns.nothingType
    private val stringType = context.irBuiltIns.stringType

    private val IrRichFunctionReference.secondFunctionInterface: IrClass?
        get() =
            // If we implement KFunctionN we also need FunctionN
            if (isKReference) {
                val referenceType = type as IrSimpleType
                val arity = referenceType.arguments.size - 1
                if (invokeFunction.isSuspend)
                    context.symbols.suspendFunctionN(arity).owner
                else
                    context.symbols.functionN(arity).owner
            } else null

    private fun StringBuilder.collectNamesForLambda(d: IrDeclarationWithName) {
        val parent = d.parent

        if (parent is IrPackageFragment) {
            append(d.name.asString())
            return
        }

        collectNamesForLambda(parent as IrDeclarationWithName)

        if (d is IrAnonymousInitializer) return

        fun IrDeclaration.isLambdaFun(): Boolean = origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA

        when {
            d.isLambdaFun() -> {
                append('$')
                if (d is IrSimpleFunction && d.isSuspend) append('s')
                append("lambda")
            }
            d.name == SpecialNames.NO_NAME_PROVIDED -> append("\$o")
            else -> {
                append('$')
                append(d.name.asString())
            }
        }
    }

    override fun getReferenceClassName(reference: IrRichFunctionReference): Name {
        val sb = StringBuilder()
        sb.collectNamesForLambda(reference.reflectionTargetSymbol?.owner ?: reference.invokeFunction)
        if (!reference.isLambda) sb.append("\$ref")
        return Name.identifier(sb.toString())
    }

    override fun getAdditionalInterfaces(reference: IrRichFunctionReference): List<IrType> =
        listOfNotNull(reference.secondFunctionInterface?.symbol?.typeWithArguments((reference.type.removeProjections() as IrSimpleType).arguments))

    override fun postprocessInvoke(invokeFunction: IrSimpleFunction, functionReference: IrRichFunctionReference) {
        val superInvokeFun = functionReference.secondFunctionInterface?.invokeFun ?: return
        invokeFunction.overriddenSymbols = invokeFunction.overriddenSymbols memoryOptimizedPlus superInvokeFun.symbol
    }

    override fun IrBuilderWithScope.getExtraConstructorArgument(
        parameter: IrValueParameter,
        reference: IrRichFunctionReference,
    ) = irNull()

    override fun getConstructorOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin {
        return GENERATED_MEMBER_IN_CALLABLE_REFERENCE
    }

    override fun getInvokeMethodOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin {
        return IrDeclarationOrigin.DEFINED
    }

    private fun createNameProperty(clazz: IrClass, reference: IrRichFunctionReference) {
        val reflectionTargetSymbol = reference.reflectionTargetSymbol ?: return
        val superProperty = reference
            .type
            .classOrFail
            .owner
            .declarations
            .filterIsInstance<IrProperty>()
            .single { it.name == StandardNames.NAME }  // In K/Wasm interfaces can have fake overridden properties from Any

        val supperGetter = superProperty.getter
            ?: compilationException(
                "Expected getter for KFunction.name property",
                superProperty
            )

        val nameProperty = clazz.addProperty {
            visibility = superProperty.visibility
            name = superProperty.name
            origin = GENERATED_MEMBER_IN_CALLABLE_REFERENCE
        }

        nameProperty.overriddenSymbols = listOf(superProperty.symbol)

        val getter = nameProperty.addGetter {
            returnType = stringType
        }
        getter.overriddenSymbols = listOf(supperGetter.symbol)
        getter.parameters += getter.createDispatchReceiverParameterWithClassParent()

        // TODO: What name should be in case of constructor? <init> or class name?
        val functionReferenceReflectedName = reflectionTargetSymbol.owner.name.asString()

        val linkageError = reference.reflectionTargetLinkageError
        val statement = if (linkageError != null) {
            val file = PartialLinkageSources.File.determineFileFor(reference.invokeFunction)
            clazz.functionReferenceLinkageError = context.partialLinkageSupport.prepareLinkageError(
                doNotLog = true,
                linkageError,
                reference,
                file,
            )
            context.partialLinkageSupport.throwLinkageError(
                linkageError,
                reference,
                file,
                doNotLog = true
            )
        } else {
            IrReturnImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, nothingType, getter.symbol, IrConstImpl.string(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, stringType, functionReferenceReflectedName
                )
            )
        }
        getter.body = context.irFactory.createBlockBody(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(statement)
        )

        clazz.functionReferenceReflectedName = functionReferenceReflectedName
    }

    override fun generateExtraMethods(functionReferenceClass: IrClass, reference: IrRichFunctionReference) {
        if (reference.isKReference) {
            createNameProperty(functionReferenceClass, reference)
        }
    }

    companion object {
        val LAMBDA_IMPL by IrDeclarationOriginImpl.Regular
        val FUNCTION_REFERENCE_IMPL by IrDeclarationOriginImpl.Regular
        val GENERATED_MEMBER_IN_CALLABLE_REFERENCE by IrDeclarationOriginImpl.Regular
    }
}
