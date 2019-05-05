/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.SpecialBridgeMethods
import org.jetbrains.kotlin.backend.common.lower.allOverridden
import org.jetbrains.kotlin.backend.common.bridges.FunctionHandle
import org.jetbrains.kotlin.backend.common.bridges.findAllReachableDeclarations
import org.jetbrains.kotlin.backend.common.bridges.findConcreteSuperDeclaration
import org.jetbrains.kotlin.backend.common.bridges.generateBridges
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.commons.Method

internal val bridgePhase = makeIrFilePhase(
    ::BridgeLowering,
    name = "Bridge",
    description = "Generate bridges"
)

private class BridgeLowering(val context: JvmBackendContext) : ClassLoweringPass {

    private val state = context.state

    private val typeMapper = state.typeMapper

    private val specialBridgeMethods = SpecialBridgeMethods(context)

    override fun lower(irClass: IrClass) {
        // TODO: Bridges should be generated for @JvmDefaults, so the interface check is too optimistic.
        if (irClass.isInterface || irClass.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) {
            return
        }

        for (member in irClass.declarations.filterIsInstance<IrSimpleFunction>()) {
            createBridges(member)
        }
    }


    private fun createBridges(irFunction: IrSimpleFunction) {
        if (irFunction.isStatic) return
        if (irFunction.isMethodOfAny()) return

        if (irFunction.origin === IrDeclarationOrigin.FAKE_OVERRIDE &&
            irFunction.overriddenSymbols.all { it.owner.modality !== Modality.ABSTRACT && !it.owner.comesFromJava() }
        ) {
            // All needed bridges will be generated where functions are implemented.
            return
        }


        val irClass = irFunction.parentAsClass
        val ourSignature = irFunction.getJvmSignature()
        val ourMethodName = ourSignature.name

        val (specialOverride, specialOverrideValueGenerator) =
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
                ?.let { (it.getJvmName() != ourMethodName || it.getJvmSignature() == specialOverrideSignature) && it.comesFromJava() } == true
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
                    defaultValueGenerator = null,
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
                specialOverrideValueGenerator,
                isSpecial = true
            )
        }

        val bridgeSignatures = generateBridges(
            FunctionHandleForIrFunction(irFunction),
            { handle -> SignatureWithSource(handle.irFunction.getJvmSignature(), handle.irFunction) }
        )

        for (bridgeSignature in bridgeSignatures) {
            val method = bridgeSignature.from.source
            addBridge(
                irClass, targetForCommonBridges, method, signaturesToSkip,
                defaultValueGenerator = null,
                isSpecial = false
            )
        }
    }

    private fun addBridge(
        irClass: IrClass,
        target: IrSimpleFunction,
        method: IrSimpleFunction,
        signaturesToSkip: MutableSet<Method>,
        defaultValueGenerator: ((IrSimpleFunction) -> IrExpression)?,
        isSpecial: Boolean
    ) {
        val signature = method.getJvmSignature()
        if (signature in signaturesToSkip) return

        val bridge = createBridgeHeader(irClass, target, method, isSpecial = isSpecial, isSynthetic = !isSpecial)
        bridge.createBridgeBody(target, defaultValueGenerator, isSpecial)
        irClass.declarations.add(bridge)
        signaturesToSkip.add(signature)
    }

    private fun IrSimpleFunction.copyRenamingTo(newName: Name): IrSimpleFunction =
        WrappedSimpleFunctionDescriptor(descriptor.annotations).let { newDescriptor ->
            IrFunctionImpl(
                startOffset, endOffset, origin,
                IrSimpleFunctionSymbolImpl(newDescriptor),
                newName,
                visibility, modality, returnType,
                isInline, isExternal, isTailrec, isSuspend
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
        val modality = if (isSpecial) Modality.FINAL else Modality.OPEN
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
            isSuspend = signatureFunction.isSuspend
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

    private fun IrSimpleFunction.createBridgeBody(
        target: IrSimpleFunction,
        defaultValueGenerator: ((IrSimpleFunction) -> IrExpression)?,
        isSpecial: Boolean,
        invokeStatically: Boolean = false
    ) {
        val maybeOrphanedTarget = if (isSpecial)
            target.orphanedCopy()
        else
            target

        context.createIrBuilder(symbol).run {
            body = irBlockBody {
                if (defaultValueGenerator != null) {
                    valueParameters.forEach {
                        +irIfThen(
                            context.irBuiltIns.unitType,
                            irNot(irIs(irGet(it), maybeOrphanedTarget.valueParameters[it.index].type)),
                            irReturn(defaultValueGenerator(this@createBridgeBody))
                        )
                    }
                }
                +irReturn(
                    irImplicitCast(
                        IrCallImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            maybeOrphanedTarget.returnType,
                            maybeOrphanedTarget.symbol, maybeOrphanedTarget.descriptor,
                            origin = IrStatementOrigin.BRIDGE_DELEGATION,
                            superQualifierSymbol = if (invokeStatically) maybeOrphanedTarget.parentAsClass.symbol else null
                        ).apply {
                            passTypeArgumentsFrom(this@createBridgeBody)
                            dispatchReceiver = irImplicitCast(irGet(dispatchReceiverParameter!!), dispatchReceiverParameter!!.type)
                            extensionReceiverParameter?.let {
                                extensionReceiver = irImplicitCast(irGet(it), extensionReceiverParameter!!.type)
                            }
                            valueParameters.forEach {
                                putValueArgument(it.index, irImplicitCast(irGet(it), maybeOrphanedTarget.valueParameters[it.index].type))
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
                    isInline, isExternal, isTailrec, isSuspend
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
        val descriptor = WrappedValueParameterDescriptor(this.descriptor.annotations)
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

    /* Perform type erasure as much as is significant for JVM signature generation. */
    // TODO: should be a type mapper functionality.
    private fun IrType.eraseTypeParameters() = when (this) {
        is IrErrorType -> this
        is IrSimpleType -> {
            val owner = classifier.owner
            when (owner) {
                is IrClass -> this
                is IrTypeParameter -> {
                    val upperBound = owner.upperBoundClass()
                    IrSimpleTypeImpl(
                        upperBound.symbol,
                        hasQuestionMark,
                        List(upperBound.typeParameters.size) { IrStarProjectionImpl },    // Should not affect JVM signature, but may result in an invalid type object
                        owner.annotations
                    )
                }
                else -> error("Unknown IrSimpleType classifier kind: $owner")
            }
        }
        else -> error("Unknown IrType kind: $this")
    }

    private fun IrTypeParameter.upperBoundClass(): IrClass {
        val simpleSuperClassifiers = superTypes.asSequence().filterIsInstance<IrSimpleType>().map { it.classifier }
        return simpleSuperClassifiers
                .filterIsInstance<IrClassSymbol>()
                .let {
                    it.firstOrNull { !it.owner.isInterface } ?: it.firstOrNull()
                }?.owner ?:
            simpleSuperClassifiers.filterIsInstance<IrTypeParameterSymbol>().map { it.owner.upperBoundClass() }.firstOrNull() ?:
            context.irBuiltIns.anyClass.owner
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
        override val mayBeUsedAsSuperImplementation get() = !irFunction.parentAsClass.isInterface

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

    private fun IrFunction.getJvmSignature() = typeMapper.mapAsmMethod(descriptor)
    private fun IrFunction.getJvmName() = getJvmSignature().name
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

// TODO: At present, there is no reliable way to distinguish Java imports from Kotlin cross-module imports.
val ORIGINS_FROM_JAVA = setOf(IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB)

fun IrDeclaration.comesFromJava() = parentAsClass.origin in ORIGINS_FROM_JAVA

// Method has the same name, same arguments as `other`. Return types may differ.
fun Method.sameCallAs(other: Method) =
    name == other.name &&
            argumentTypes?.contentEquals(other.argumentTypes) == true
