/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.bridges.FunctionHandle
import org.jetbrains.kotlin.backend.common.bridges.findAllReachableDeclarations
import org.jetbrains.kotlin.backend.common.bridges.findConcreteSuperDeclaration
import org.jetbrains.kotlin.backend.common.bridges.generateBridges
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.eraseTypeParameters
import org.jetbrains.kotlin.backend.jvm.ir.hasJvmDefault
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

internal val bridgePhase = makeIrFilePhase(
    ::BridgeLowering,
    name = "Bridge",
    description = "Generate bridges"
)

private class BridgeLowering(val context: JvmBackendContext) : ClassLoweringPass {
    private val methodSignatureMapper = context.methodSignatureMapper

    private val specialBridgeMethods = SpecialBridgeMethods(context)

    override fun lower(irClass: IrClass) {
        if (irClass.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
            return
        }

        for (member in irClass.declarations.filterIsInstance<IrSimpleFunction>()) {
            if (!irClass.isInterface || member.hasJvmDefault())
                createBridges(member)
        }
    }


    private fun createBridges(irFunction: IrSimpleFunction) {
        if (irFunction.isStatic) return
        if (irFunction.isMethodOfAny()) return

        if (irFunction.origin === IrDeclarationOrigin.FAKE_OVERRIDE &&
            irFunction.overriddenSymbols.all {
                !it.owner.comesFromJava() &&
                        if ((it.owner.parent as? IrClass)?.isInterface == true)
                            it.owner.hasJvmDefault() // TODO: Remove this after modality is corrected in InterfaceLowering.
                        else
                            it.owner.modality !== Modality.ABSTRACT
            }
        ) {
            // All needed bridges will be generated where functions are implemented.
            return
        }

        val irClass = irFunction.parentAsClass
        val ourSignature = irFunction.getJvmSignature()
        val ourMethodName = ourSignature.name

        val (specialOverride, specialOverrideInfo) =
            specialBridgeMethods.findSpecialWithOverride(irFunction) ?: Pair(null, null)
        val specialOverrideSignature = specialOverride?.getJvmSignature()

        var targetForCommonBridges = irFunction

        // Special case: fake override redirecting to an implementation with a different JVM name,
        // or to a function with SpecialOverrideSignature.
        // TODO: we assume here that all implementations come from classes. There may be a default implementation in
        // an interface, If it comes from the same module, InterfaceDelegationLowering will build a redirection, and the following code will work.
        // But in an imported module, there will be no redirection => failure!
        if (irFunction.origin === IrDeclarationOrigin.FAKE_OVERRIDE &&
            irFunction.modality !== Modality.ABSTRACT &&
            irFunction.visibility !== Visibilities.INVISIBLE_FAKE &&
            irFunction.overriddenInClasses().firstOrNull { it.getJvmSignature() != ourSignature || it.origin != IrDeclarationOrigin.FAKE_OVERRIDE }
                ?.let { (it.getJvmName() != ourMethodName || it.getJvmSignature() == specialOverrideSignature) && it.isExternalDeclaration() } == true
        ) {
            val resolved = irFunction.findConcreteSuperDeclaration()!!
            val resolvedSignature = resolved.getJvmSignature()
            if (!resolvedSignature.sameCallAs(ourSignature)) {
                val bridge = createBridgeHeader(irClass, resolved, irFunction, isSpecial = false, isSynthetic = false)
                bridge.createBridgeBody(resolved, null, isSpecial = false, invokeStatically = true)
                irClass.declarations.add(bridge)
                targetForCommonBridges = bridge
            }
        } else if (irFunction.origin == IrDeclarationOrigin.FAKE_OVERRIDE &&
            irFunction.modality == Modality.ABSTRACT &&
            irFunction.overriddenSymbols.all { it.owner.getJvmName() != ourMethodName }
        ) {
            // Bridges for abstract fake overrides whose JVM names differ from overridden functions.
            // The orphaned copy will get irFunction's name; the original irFunction will be renamed
            // according to its overrides,
            val bridge = irFunction.orphanedCopy()
            irClass.declarations.add(bridge)
            targetForCommonBridges = bridge
        }

        val signaturesToSkip = mutableSetOf(ourSignature)
        signaturesToSkip.addAll(getFinalOverridden(irFunction).map { it.getJvmSignature() })

        val firstOverridden = irFunction.overriddenInClasses().firstOrNull()
        val firstOverriddenSignature = firstOverridden?.getJvmSignature()

        val renamedOverridden = getRenamedOverridden(irFunction)
        if (renamedOverridden != null) {
            val renamer = irFunction.copyRenamingTo(Name.identifier(renamedOverridden.getJvmName()))
            // Renaming bridge may have already been generated in parent.
            if (firstOverridden == null || firstOverriddenSignature!!.name != ourMethodName || getRenamedOverridden(firstOverridden) == null) {
                addBridge(
                    irClass, targetForCommonBridges, renamer, signaturesToSkip,
                    specialOverrideInfo = null,
                    isSpecial = true
                )
            } else {
                // Renamer bridge in superclass.
                signaturesToSkip.add(renamer.getJvmSignature())
            }
        }

        if (specialOverride != null && (firstOverridden == null || firstOverriddenSignature != ourSignature) &&
            specialOverrideSignature !in signaturesToSkip
        ) {
            addBridge(
                irClass, targetForCommonBridges, specialOverride, signaturesToSkip,
                specialOverrideInfo,
                isSpecial = true
            )
        }

        // Deal with existing function that override special bridge methods.
        if (!irFunction.isFakeOverride && specialOverride != null) {
            irFunction.rewriteSpecialMethodBody(ourSignature, specialOverrideSignature!!, specialOverrideInfo!!)
        }

        val bridgeSignatures = generateBridges(
            FunctionHandleForIrFunction(irFunction),
            { handle -> SignatureWithSource(handle.irFunction.getJvmSignature(), handle.irFunction) }
        )

        for (bridgeSignature in bridgeSignatures) {
            val method = bridgeSignature.from.source
            addBridge(
                irClass, targetForCommonBridges, method, signaturesToSkip,
                specialOverrideInfo = null,
                isSpecial = targetForCommonBridges.isCollectionStub()
            )
        }
    }

    private fun addBridge(
        irClass: IrClass,
        target: IrSimpleFunction,
        method: IrSimpleFunction,
        signaturesToSkip: MutableSet<Method>,
        specialOverrideInfo: SpecialMethodWithDefaultInfo?,
        isSpecial: Boolean
    ) {
        val signature = method.getJvmSignature()
        if (signature in signaturesToSkip) return

        val bridge = createBridgeHeader(irClass, target, method, isSpecial = isSpecial, isSynthetic = !isSpecial)
        bridge.createBridgeBody(target, specialOverrideInfo, isSpecial)
        irClass.declarations.add(bridge)

        // For lambda classes, we move override from the `invoke` function to its bridge. This will allow us to avoid boxing
        // the return type of `invoke` in codegen, in case lambda's return type is primitive.
        if (method.name == OperatorNameConventions.INVOKE && irClass.origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL) {
            target.overriddenSymbols.remove(method.symbol)
            bridge.overriddenSymbols.add(method.symbol)
        }

        signaturesToSkip.add(signature)
    }

    private fun IrSimpleFunction.copyRenamingTo(newName: Name): IrSimpleFunction =
        WrappedSimpleFunctionDescriptor(descriptor.annotations).let { newDescriptor ->
            IrFunctionImpl(
                startOffset, endOffset, origin,
                IrSimpleFunctionSymbolImpl(newDescriptor),
                newName,
                visibility, modality, returnType,
                isInline = isInline, isExternal = isExternal, isTailrec = isTailrec, isSuspend = isSuspend, isExpect = isExpect,
                isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
                isOperator = isOperator
            ).apply {
                newDescriptor.bind(this)
                parent = this@copyRenamingTo.parent
                dispatchReceiverParameter = this@copyRenamingTo.dispatchReceiverParameter?.copyTo(this)
                extensionReceiverParameter = this@copyRenamingTo.extensionReceiverParameter?.copyTo(this)
                valueParameters.addAll(this@copyRenamingTo.valueParameters.map { it.copyTo(this) })
            }
        }

    private fun createBridgeHeader(
        irClass: IrClass,
        target: IrSimpleFunction,
        signatureFunction: IrSimpleFunction,
        isSpecial: Boolean,
        isSynthetic: Boolean
    ): IrSimpleFunction {
        val modality = if (isSpecial && !target.isCollectionStub()) Modality.FINAL else Modality.OPEN
        val origin = if (isSynthetic) IrDeclarationOrigin.BRIDGE else IrDeclarationOrigin.BRIDGE_SPECIAL

        val visibility = if (signatureFunction.visibility === Visibilities.INTERNAL) Visibilities.PUBLIC else signatureFunction.visibility
        val descriptor = WrappedSimpleFunctionDescriptor()
        return IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            origin,
            IrSimpleFunctionSymbolImpl(descriptor),
            Name.identifier(signatureFunction.getJvmName()),
            visibility,
            modality,
            returnType = signatureFunction.returnType.eraseTypeParameters(),
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = signatureFunction.isSuspend,
            isExpect = false,
            isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
            isOperator = false
        ).apply {
            descriptor.bind(this)
            parent = irClass
            copyTypeParametersFrom(target)

            // Have to specify type explicitly to prevent an attempt to remap it.
            dispatchReceiverParameter = irClass.thisReceiver?.copyTo(this, type = irClass.defaultType)
            extensionReceiverParameter = signatureFunction.extensionReceiverParameter
                ?.copyWithTypeErasure(this)
            signatureFunction.valueParameters.mapIndexed { i, param ->
                valueParameters.add(i, param.copyWithTypeErasure(this))
            }
        }
    }

    private fun IrStatementsBuilder<IrBlockBody>.addParameterTypeCheck(
        parameter: IrValueParameter,
        type: IrType,
        defaultValueGenerator: ((IrSimpleFunction) -> IrExpression),
        function: IrSimpleFunction
    ) {
        +irIfThen(
            context.irBuiltIns.unitType,
            irNot(irIs(irGet(parameter), type)),
            irReturn(defaultValueGenerator(function))
        )
    }

    private fun IrSimpleFunction.rewriteSpecialMethodBody(
        ourSignature: Method,
        specialOverrideSignature: Method,
        specialOverrideInfo: SpecialMethodWithDefaultInfo
    ) {
        // If there is an existing function that would conflict with a special bridge signature, insert the special bridge
        // code directly as a prelude in the existing method.
        val variableMap = mutableMapOf<IrValueParameter, IrValueParameter>()
        if (specialOverrideSignature == ourSignature) {
            val argumentsToCheck = valueParameters.take(specialOverrideInfo.argumentsToCheck)
            val shouldGenerateParameterChecks = argumentsToCheck.any { !it.type.isNullable() }
            if (shouldGenerateParameterChecks) {
                // Rewrite the body to check if arguments have wrong type. If so, return the default value, otherwise,
                // use the existing function body.
                context.createIrBuilder(symbol).run {
                    body = irBlockBody {
                        // Change the parameter types to be Any? so that null checks are not generated. The checks
                        // we insert here make them superfluous.
                        argumentsToCheck.forEach {
                            val parameterType = it.type
                            if (!parameterType.isNullable()) {
                                val newParameter = it.copyTo(this@rewriteSpecialMethodBody, type = context.irBuiltIns.anyNType)
                                variableMap.put(valueParameters[it.index], newParameter)
                                valueParameters[it.index] = newParameter
                                addParameterTypeCheck(
                                    newParameter,
                                    parameterType,
                                    specialOverrideInfo.defaultValueGenerator,
                                    this@rewriteSpecialMethodBody
                                )
                            }
                        }
                        // After the checks, insert the orignal method body.
                        if (body is IrExpressionBody) {
                            +irReturn((body as IrExpressionBody).expression)
                        } else {
                            (body as IrBlockBody).statements.forEach { +it }
                        }
                    }
                }
            }
        } else {
            // If the signature of this method will be changed in the output to take a boxed argument instead of a primitive,
            // rewrite the argument so that code will be generated for a boxed argument and not a primitive.
            for ((i, p) in valueParameters.withIndex()) {
                if (AsmUtil.isPrimitive(context.typeMapper.mapType(p.type)) && ourSignature.argumentTypes[i].sort == Type.OBJECT) {
                    val newParameter = p.copyTo(this, type = p.type.makeNullable())
                    variableMap[p] = newParameter
                    valueParameters[i] = newParameter
                }
            }
        }
        // If any parameters change, remap them in the function body.
        if (variableMap.isNotEmpty()) {
            body?.transform(VariableRemapper(variableMap), null)
        }

    }

    private fun IrSimpleFunction.createBridgeBody(
        target: IrSimpleFunction,
        specialMethodInfo: SpecialMethodWithDefaultInfo?,
        isSpecial: Boolean,
        invokeStatically: Boolean = false
    ) {
        context.createIrBuilder(symbol).run {
            body = irBlockBody {
                if (specialMethodInfo != null) {
                    valueParameters.take(specialMethodInfo.argumentsToCheck).forEach {
                        addParameterTypeCheck(
                            it,
                            target.valueParameters[it.index].type,
                            specialMethodInfo.defaultValueGenerator,
                            this@createBridgeBody
                        )
                    }
                }
                +irReturn(
                    irImplicitCast(
                        IrCallImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            target.returnType,
                            target.symbol, origin = IrStatementOrigin.BRIDGE_DELEGATION,
                            superQualifierSymbol = if (invokeStatically) target.parentAsClass.symbol else null
                        ).apply {
                            passTypeArgumentsFrom(this@createBridgeBody)
                            dispatchReceiver = irGet(dispatchReceiverParameter!!)
                            extensionReceiverParameter?.let {
                                extensionReceiver = irImplicitCast(irGet(it), target.extensionReceiverParameter!!.type)
                            }
                            valueParameters.forEach {
                                putValueArgument(it.index, irImplicitCast(irGet(it), target.valueParameters[it.index].type))
                            }
                        },
                        returnType
                    )
                )
            }
        }
    }

    /* A hacky way to make sure the code generator calls the right function, and not some standard interface it implements. */
    private fun IrSimpleFunction.orphanedCopy() =
        if (overriddenSymbols.size == 0)
            this
        else
            WrappedSimpleFunctionDescriptor(descriptor.annotations).let { wrappedDescriptor ->
                val newOrigin = if (origin == IrDeclarationOrigin.FAKE_OVERRIDE) IrDeclarationOrigin.DEFINED else origin
                IrFunctionImpl(
                    startOffset, endOffset, newOrigin,
                    IrSimpleFunctionSymbolImpl(wrappedDescriptor),
                    Name.identifier(getJvmName()),
                    visibility, modality, returnType,
                    isInline = isInline, isExternal = isExternal, isTailrec = isTailrec, isSuspend = isSuspend, isExpect = isExpect,
                    isFakeOverride = newOrigin == IrDeclarationOrigin.FAKE_OVERRIDE,
                    isOperator = isOperator
                ).apply {
                    wrappedDescriptor.bind(this)
                    parent = this@orphanedCopy.parent
                    copyTypeParametersFrom(this@orphanedCopy)
                    this@orphanedCopy.dispatchReceiverParameter?.let { dispatchReceiverParameter = it.copyTo(this) }
                    this@orphanedCopy.extensionReceiverParameter?.let { extensionReceiverParameter = it.copyTo(this) }
                    this@orphanedCopy.valueParameters.forEachIndexed { index, param ->
                        valueParameters.add(index, param.copyTo(this))
                    }
                    /* Do NOT copy overriddenSymbols */
                }
            }

    private fun IrValueParameter.copyWithTypeErasure(target: IrSimpleFunction): IrValueParameter {
        val descriptor = if (this.descriptor is ReceiverParameterDescriptor) {
            WrappedReceiverParameterDescriptor(this.descriptor.annotations)
        } else {
            WrappedValueParameterDescriptor(this.descriptor.annotations)
        }
        return IrValueParameterImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.BRIDGE,
            IrValueParameterSymbolImpl(descriptor),
            name,
            index,
            type.eraseTypeParameters(),
            varargElementType?.eraseTypeParameters(),
            isCrossinline,
            isNoinline
        ).apply {
            descriptor.bind(this)
            parent = target
        }
    }

    private fun IrSimpleFunction.findAllReachableDeclarations() =
        findAllReachableDeclarations(FunctionHandleForIrFunction(this)).map { it.irFunction }

    private fun getFinalOverridden(irFunction: IrSimpleFunction): List<IrSimpleFunction> {
        return irFunction.findAllReachableDeclarations().filter { it.modality === Modality.FINAL }
    }

    // There are two sources of method name change:
    //   1. Special methods renamed from java
    //   2. Internal methods overridden by public ones.
    // Here, we want to only deal with the first case.
    private fun getRenamedOverridden(irFunction: IrSimpleFunction): IrSimpleFunction? {
        val ourName = irFunction.getJvmName()
        return irFunction.allOverridden().firstOrNull {
            it.visibility == Visibilities.PUBLIC && it.getJvmName() != ourName
        }
    }


    private inner class FunctionHandleForIrFunction(val irFunction: IrSimpleFunction) : FunctionHandle {
        override val isDeclaration get() = irFunction.origin != IrDeclarationOrigin.FAKE_OVERRIDE
        override val isAbstract get() = irFunction.modality == Modality.ABSTRACT
        override val mayBeUsedAsSuperImplementation get() = !irFunction.parentAsClass.isInterface || irFunction.hasJvmDefault()

        override fun getOverridden() = irFunction.overriddenSymbols.map { FunctionHandleForIrFunction(it.owner) }

        override fun hashCode(): Int =
            irFunction.parent.safeAs<IrClass>()?.fqNameWhenAvailable.hashCode() + 31 * irFunction.getJvmSignature().hashCode()

        override fun equals(other: Any?): Boolean =
            other is FunctionHandleForIrFunction &&
                    irFunction.parent.safeAs<IrClass>()?.fqNameWhenAvailable == other.irFunction.parent.safeAs<IrClass>()?.fqNameWhenAvailable &&
                    irFunction.getJvmSignature() == other.irFunction.getJvmSignature()
    }

    fun IrSimpleFunction.findConcreteSuperDeclaration(): IrSimpleFunction? {
        return findConcreteSuperDeclaration(FunctionHandleForIrFunction(this))?.irFunction
    }

    private fun IrFunction.getJvmSignature(): Method = methodSignatureMapper.mapAsmMethod(this)
    private fun IrFunction.getJvmName(): String = getJvmSignature().name
}

private data class SignatureWithSource(val signature: Method, val source: IrSimpleFunction) {
    override fun hashCode(): Int {
        return signature.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is SignatureWithSource && signature == other.signature
    }
}


fun IrSimpleFunction.overriddenInClasses(): Sequence<IrSimpleFunction> =
    allOverridden().filter { !(it.parent.safeAs<IrClass>()?.isInterface ?: true) }

fun IrSimpleFunction.isCollectionStub(): Boolean =
    origin == IrDeclarationOrigin.IR_BUILTINS_STUB

val EXTERNAL_ORIGIN = setOf(IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB)

fun IrDeclaration.comesFromJava() = parentAsClass.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

fun IrDeclaration.isExternalDeclaration() = parentAsClass.origin in EXTERNAL_ORIGIN

// Method has the same name, same arguments as `other`. Return types may differ.
fun Method.sameCallAs(other: Method) =
    name == other.name &&
            argumentTypes?.contentEquals(other.argumentTypes) == true
