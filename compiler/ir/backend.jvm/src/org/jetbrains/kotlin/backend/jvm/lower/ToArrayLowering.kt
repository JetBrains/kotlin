/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.KnownClassDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.KnownPackageFragmentDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.makePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.isGenericToArray
import org.jetbrains.kotlin.codegen.isNonGenericToArray
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils.isSubclass
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.types.Variance

class ToArrayLowering(private val context: JvmBackendContext) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        if (irClass.isJvmInterface) return

        val descriptor = irClass.descriptor

        val builtIns = descriptor.builtIns
        if (!isSubclass(descriptor, builtIns.collection)) return

        if (descriptor.getAllSuperclassesWithoutAny().any { classDescriptor ->
                classDescriptor !is JavaClassDescriptor && isSubclass(
                    classDescriptor,
                    builtIns.collection
                )
            }) {
            return
        }

        val toArrayName = Name.identifier("toArray")
        val functions = descriptor.defaultType.memberScope.getContributedFunctions(
            toArrayName, NoLookupLocation.FROM_BACKEND
        )

        val genericToArray = functions.firstOrNull { it.isGenericToArray() }
        val nonGenericToArray = functions.firstOrNull { it.isNonGenericToArray() }
        val arrayType = builtIns.getArrayType(Variance.INVARIANT, builtIns.anyType)
        if (genericToArray == null) {
            val toArrayDescriptor = SimpleFunctionDescriptorImpl.create(
                irClass.descriptor, org.jetbrains.kotlin.descriptors.annotations.Annotations.EMPTY,
                toArrayName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
            )
            toArrayDescriptor.initialize(
                null, irClass.descriptor.thisAsReceiverParameter, emptyList(), emptyList(),
                arrayType, Modality.OPEN, Visibilities.PUBLIC
            )

            val toArrayUtilDescriptor = createToArrayUtilDescriptor(builtIns, false)
            val toArrayType = toArrayUtilDescriptor.returnType!!.toIrType()!!

            val irFunction = IrFunctionImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                JvmLoweredDeclarationOrigin.TO_ARRAY,
                toArrayDescriptor,
                returnType = toArrayType
            )
            irFunction.createParameterDeclarations()

            irFunction.body = context.createIrBuilder(irFunction.symbol).irBlockBody {
                +irReturn(
                    irCall(IrSimpleFunctionSymbolImpl(toArrayUtilDescriptor), toArrayType).apply {
                        putValueArgument(
                            0,
                            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunction.dispatchReceiverParameter!!.symbol)
                        )
                    })
            }
            irClass.declarations.add(irFunction)
        } else {
            //TODO patch visibility
        }

        if (nonGenericToArray == null) {
            val toArrayDescriptor = SimpleFunctionDescriptorImpl.create(
                irClass.descriptor, org.jetbrains.kotlin.descriptors.annotations.Annotations.EMPTY,
                toArrayName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
            )
            val createWithDefaultBound = TypeParameterDescriptorImpl.createWithDefaultBound(
                toArrayDescriptor,
                Annotations.EMPTY,
                false,
                Variance.INVARIANT,
                Name.identifier("T"),
                0
            )

            val genericArrayType = builtIns.getArrayType(Variance.INVARIANT, createWithDefaultBound.defaultType)
            toArrayDescriptor.initialize(
                null,
                irClass.descriptor.thisAsReceiverParameter,
                listOf(createWithDefaultBound),
                listOf(
                    ValueParameterDescriptorImpl(
                        toArrayDescriptor,
                        null,
                        0,
                        Annotations.EMPTY,
                        Name.identifier("array"),
                        genericArrayType,
                        false,
                        false,
                        false,
                        null,
                        SourceElement.NO_SOURCE
                    )
                ),
                genericArrayType,
                Modality.OPEN,
                Visibilities.PUBLIC
            )

            val toArrayUtilDescriptor = createToArrayUtilDescriptor(builtIns, true)
            val toArrayType = toArrayUtilDescriptor.returnType!!.toIrType()!!

            val irFunction = IrFunctionImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                JvmLoweredDeclarationOrigin.TO_ARRAY,
                toArrayDescriptor,
                toArrayType
            )
            irFunction.createParameterDeclarations()

            irFunction.body = context.createIrBuilder(irFunction.symbol).irBlockBody {
                +irReturn(
                    irCall(IrSimpleFunctionSymbolImpl(toArrayUtilDescriptor), toArrayType).apply {
                        putValueArgument(
                            0,
                            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunction.dispatchReceiverParameter!!.symbol)
                        )
                        putValueArgument(1, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunction.valueParameters[0].symbol))
                    })
            }

            irClass.declarations.add(irFunction)
        } else {
            //TODO patch visibility
        }
    }

    private fun createToArrayUtilDescriptor(
        builtIns: KotlinBuiltIns,
        isGeneric: Boolean
    ): SimpleFunctionDescriptorImpl {
        val kotlinJvmInternalPackage = KnownPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.jvm.internal"))

        val toArrayUtil = SimpleFunctionDescriptorImpl.create(
            KnownClassDescriptor.createClass(Name.identifier("CollectionToArray"), kotlinJvmInternalPackage, listOf(builtIns.anyType)),
            Annotations.EMPTY,
            Name.identifier("toArray"),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        )
        toArrayUtil.initialize(
            null,
            null,
            emptyList(),
            listOfNotNull(
                ValueParameterDescriptorImpl(
                    toArrayUtil,
                    null,
                    0,
                    Annotations.EMPTY,
                    Name.identifier("collection"),
                    builtIns.collection.defaultType,
                    false,
                    false,
                    false,
                    null,
                    SourceElement.NO_SOURCE
                ),
                if (isGeneric) ValueParameterDescriptorImpl(
                    toArrayUtil,
                    null,
                    1,
                    Annotations.EMPTY,
                    Name.identifier("array"),
                    builtIns.getArrayType(Variance.INVARIANT, builtIns.anyType),
                    false,
                    false,
                    false,
                    null,
                    SourceElement.NO_SOURCE
                ) else null
            ),
            builtIns.array.defaultType,
            Modality.OPEN,
            Visibilities.PUBLIC
        )
        return toArrayUtil
    }
}