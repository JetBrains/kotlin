/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.bridges.Bridge
import org.jetbrains.kotlin.backend.common.bridges.findInterfaceImplementation
import org.jetbrains.kotlin.backend.common.bridges.generateBridgesForFunctionDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.descriptors.DefaultImplsClassDescriptor
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmFunctionDescriptorImpl
import org.jetbrains.kotlin.codegen.AsmUtil.isAbstractMethod
import org.jetbrains.kotlin.codegen.BuiltinSpecialBridgesUtil
import org.jetbrains.kotlin.codegen.FunctionCodegen.isMethodOfAny
import org.jetbrains.kotlin.codegen.FunctionCodegen.isThereOverriddenInKotlinClass
import org.jetbrains.kotlin.codegen.JvmCodegenUtil.getDirectMember
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.descriptors.FileClassDescriptor
import org.jetbrains.kotlin.codegen.isToArrayFromCollection
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.load.java.getOverriddenBuiltinReflectingJvmDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.getSourceFromDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.getSuperClassDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils.isInterface
import org.jetbrains.kotlin.resolve.annotations.hasJvmDefaultAnnotation
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class BridgeLowering(val context: JvmBackendContext) : ClassLoweringPass {

    private val state = context.state

    private val typeMapper = state.typeMapper

    private val DECLARATION_AND_DEFINITION_CHECKER = fun(descriptor: CallableMemberDescriptor): Boolean =
        !isInterface(descriptor.containingDeclaration) || state.target !== JvmTarget.JVM_1_6 && descriptor.hasJvmDefaultAnnotation()

    override fun lower(irClass: IrClass) {
        val classDescriptor = irClass.descriptor
        if (classDescriptor is FileClassDescriptor) return

        if (classDescriptor is DefaultImplsClassDescriptor) {
            return /*TODO?*/
        }

        val functions = irClass.declarations.filterIsInstance<IrFunction>().filterNot {
            val descriptor = it.descriptor
            descriptor is ConstructorDescriptor ||
                    DescriptorUtils.isStaticDeclaration(descriptor) ||
                    !descriptor.kind.isReal
        }

        functions.forEach {
            generateBridges(it.descriptor, irClass)
        }


        //additional bridges for inherited interface methods
        if (!DescriptorUtils.isInterface(classDescriptor) && classDescriptor !is DefaultImplsClassDescriptor) {
            for (memberDescriptor in DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope)) {
                if (memberDescriptor is CallableMemberDescriptor) {
                    if (!memberDescriptor.kind.isReal && findInterfaceImplementation(memberDescriptor) == null) {
                        if (memberDescriptor is FunctionDescriptor) {
                            generateBridges(memberDescriptor, irClass)
                        } else if (memberDescriptor is PropertyDescriptor) {
                            val getter = memberDescriptor.getter
                            if (getter != null) {
                                generateBridges(getter, irClass)
                            }
                            val setter = memberDescriptor.setter
                            if (setter != null) {
                                generateBridges(setter, irClass)
                            }
                        }
                    }
                }
            }
        }
    }


    private fun generateBridges(descriptor: FunctionDescriptor, irClass: IrClass) {
        // equals(Any?), hashCode(), toString() never need bridges
        if (isMethodOfAny(descriptor)) {
            return
        }
        val isSpecial = descriptor.getOverriddenBuiltinReflectingJvmDescriptor<CallableMemberDescriptor>() != null

        val bridgesToGenerate: Set<Bridge<SignatureAndDescriptor>>
        if (!isSpecial) {
            bridgesToGenerate = generateBridgesForFunctionDescriptor(
                descriptor,
                getSignatureMapper(typeMapper),
                DECLARATION_AND_DEFINITION_CHECKER
            )
            if (!bridgesToGenerate.isEmpty()) {
                val origin = if (descriptor.kind == DECLARATION) getSourceFromDescriptor(descriptor) else null
                val isSpecialBridge =
                    BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(descriptor) != null

                for (bridge in bridgesToGenerate) {
                    irClass.declarations.add(createBridge(origin, descriptor, bridge.from, bridge.to, isSpecialBridge, false))
                }
            }
        } else {
            val specials = BuiltinSpecialBridgesUtil.generateBridgesForBuiltinSpecial(
                descriptor,
                getSignatureMapper(typeMapper),
                DECLARATION_AND_DEFINITION_CHECKER
            )

            if (!specials.isEmpty()) {
                val origin = if (descriptor.kind == DECLARATION) getSourceFromDescriptor(descriptor) else null
                for (bridge in specials) {
                    irClass.declarations.add(
                        createBridge(
                            origin, descriptor, bridge.from, bridge.to,
                            bridge.isSpecial, bridge.isDelegateToSuper
                        )
                    )
                }
            }

            if (!descriptor.kind.isReal && isAbstractMethod(descriptor, OwnerKind.IMPLEMENTATION)) {
                descriptor.getOverriddenBuiltinReflectingJvmDescriptor<CallableMemberDescriptor>()
                        ?: error("Expect to find overridden descriptors for $descriptor")

                if (!isThereOverriddenInKotlinClass(descriptor)) {
                    // TODO: reimplement getVisibilityAccessFlag(descriptor)
                    val visibility = if (descriptor.isToArrayFromCollection()) Visibilities.PUBLIC else descriptor.visibility
                    val irFunction = IrFunctionImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        IrDeclarationOrigin.DEFINED,
                        IrSimpleFunctionSymbolImpl(descriptor),
                        visibility = visibility,
                        modality = Modality.ABSTRACT
                    )
                    irFunction.createParameterDeclarations()

                    irClass.declarations.add(irFunction)
                }
            }
        }
    }

    private fun createBridge(
        origin: PsiElement?,
        descriptor: FunctionDescriptor,
        bridge: SignatureAndDescriptor,
        delegateTo: SignatureAndDescriptor,
        isSpecialBridge: Boolean,
        isStubDeclarationWithDelegationToSuper: Boolean
    ): IrFunction {
        val isSpecialOrDelegationToSuper = isSpecialBridge || isStubDeclarationWithDelegationToSuper
        val flags =
            ACC_PUBLIC or ACC_BRIDGE or (if (!isSpecialOrDelegationToSuper) ACC_SYNTHETIC else 0) or if (isSpecialBridge) ACC_FINAL else 0 // TODO.
        val containingClass = descriptor.containingDeclaration as ClassDescriptor
        //here some 'isSpecialBridge' magic
        val bridgeDescriptorForIrFunction = JvmFunctionDescriptorImpl(
            containingClass, null, Annotations.EMPTY, Name.identifier(bridge.method.name),
            CallableMemberDescriptor.Kind.SYNTHESIZED, descriptor.source, flags
        )

        bridgeDescriptorForIrFunction.initialize(
            bridge.descriptor.extensionReceiverParameter?.copy(bridgeDescriptorForIrFunction),
            containingClass.thisAsReceiverParameter, emptyList(),
            bridge.descriptor.valueParameters.map { it.copy(bridgeDescriptorForIrFunction, it.name, it.index) },
            bridge.descriptor.returnType, Modality.OPEN, descriptor.visibility
        )

        val irFunction = IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.BRIDGE, bridgeDescriptorForIrFunction)
        irFunction.returnType = bridgeDescriptorForIrFunction.returnType!!.toIrType()!!
        irFunction.createParameterDeclarations()

        context.createIrBuilder(irFunction.symbol).irBlockBody(irFunction) {
            //TODO
            //MemberCodegen.markLineNumberForDescriptor(owner.getThisDescriptor(), iv)
            if (delegateTo.method.argumentTypes.isNotEmpty() && isSpecialBridge) {
                generateTypeCheckBarrierIfNeeded(descriptor, bridgeDescriptorForIrFunction, irFunction, delegateTo.method.argumentTypes)
            }

            val implementation = if (isSpecialBridge) delegateTo.copyAsDeclaration() else delegateTo.descriptor
            val call = IrCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                implementation.returnType!!.toIrType()!!,
                implementation,
                implementation.typeParametersCount,
                IrStatementOrigin.BRIDGE_DELEGATION,
                if (isStubDeclarationWithDelegationToSuper) getSuperClassDescriptor(
                    descriptor.containingDeclaration as ClassDescriptor
                ) else null
            )
            call.dispatchReceiver = IrGetValueImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                irFunction.dispatchReceiverParameter!!.symbol,
                IrStatementOrigin.BRIDGE_DELEGATION
            )
            irFunction.extensionReceiverParameter?.let {
                call.extensionReceiver = IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    it.symbol,
                    IrStatementOrigin.BRIDGE_DELEGATION
                )
            }
            irFunction.valueParameters.mapIndexed { i, valueParameter ->
                call.putValueArgument(
                    i,
                    IrGetValueImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        valueParameter.symbol,
                        IrStatementOrigin.BRIDGE_DELEGATION
                    )
                )
            }
            +IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunction.returnType, irFunction.symbol, call)
        }.apply {
            irFunction.body = this
        }

        return irFunction
    }

    private fun IrBlockBodyBuilder.generateTypeCheckBarrierIfNeeded(
        overrideDescriptor: FunctionDescriptor,
        bridgeDescriptor: FunctionDescriptor,
        bridgeFunction: IrFunction,
        delegateParameterTypes: Array<Type>?
    ) {
        val typeSafeBarrierDescription =
            BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(overrideDescriptor) ?: return

        BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(overrideDescriptor)
                ?: error("Overridden built-in method should not be null for $overrideDescriptor")

        val conditions = bridgeDescriptor.valueParameters.withIndex().filter { (i, parameterDescriptor) ->
            typeSafeBarrierDescription.checkParameter(i) ||
                    !(delegateParameterTypes == null || OBJECT_TYPE == delegateParameterTypes[i]) ||
                    !TypeUtils.isNullableType(parameterDescriptor.type)
        }.map { (i, _) ->
            val checkValue =
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    bridgeFunction.valueParameters[i].symbol,
                    IrStatementOrigin.BRIDGE_DELEGATION
                )
            if (delegateParameterTypes == null || OBJECT_TYPE == delegateParameterTypes[i]) {
                irNotEquals(checkValue, irNull())
            } else {
                irIs(checkValue, overrideDescriptor.valueParameters[i].type.toIrType()!!)
            }
        }

        if (conditions.isNotEmpty()) {
            val condition = conditions.fold<IrExpression, IrExpression>(irTrue()) { arg, result ->
                context.andand(arg, result)
            }

            +irIfThen(context.irBuiltIns.unitType, irNot(condition), irBlock {
                +irReturn(
                    when (typeSafeBarrierDescription) {
                        BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription.MAP_GET_OR_DEFAULT -> irGet(
                            bridgeDescriptor.valueParameters[1].type.toIrType()!!,
                            IrVariableSymbolImpl(
                                bridgeDescriptor.valueParameters[1]
                            )
                        )
                        BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription.NULL -> irNull()
                        BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription.INDEX -> IrConstImpl.int(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.intType, typeSafeBarrierDescription.defaultValue as Int
                        )
                        BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription.FALSE -> irFalse()
                    }
                )
            }
            )
        }
    }


    companion object {
        fun getSignatureMapper(typeMapper: KotlinTypeMapper): Function1<FunctionDescriptor, SignatureAndDescriptor> {
            return fun(descriptor: FunctionDescriptor) =
                SignatureAndDescriptor(typeMapper.mapAsmMethod(descriptor), descriptor)
        }

        fun SignatureAndDescriptor.copyAsDeclaration(): FunctionDescriptor {
            val containingClass = getDirectMember(descriptor).containingDeclaration as ClassDescriptor
            val delegationDescriptor = JvmFunctionDescriptorImpl(
                containingClass, null, Annotations.EMPTY, Name.identifier(method.name),
                CallableMemberDescriptor.Kind.SYNTHESIZED, descriptor.source, 0
            )

            delegationDescriptor.initialize(
                descriptor.extensionReceiverParameter?.copy(delegationDescriptor), containingClass.thisAsReceiverParameter, emptyList(),
                descriptor.valueParameters.map { it.copy(delegationDescriptor, it.name, it.index) },
                descriptor.returnType, Modality.OPEN, descriptor.visibility
            )
            return delegationDescriptor
        }
    }


    class SignatureAndDescriptor(val method: Method, val descriptor: FunctionDescriptor) {
        override fun equals(other: Any?): Boolean {
            val sig2 = other as SignatureAndDescriptor
            return method == sig2.method
        }

        override fun hashCode(): Int = method.hashCode()
    }

}

