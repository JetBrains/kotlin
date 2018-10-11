/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.common.descriptors.KnownClassDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.KnownPackageFragmentDescriptor
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*

private val SHARED_VARIABLE_ORIGIN = object : IrDeclarationOriginImpl("SHARED_VARIABLE_ORIGIN") {}

class JvmSharedVariablesManager(
    val builtIns: KotlinBuiltIns,
    val irBuiltIns: IrBuiltIns
) : SharedVariablesManager {
    private val kotlinJvmInternalPackage = KnownPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.jvm.internal"))
    private val refNamespaceClass =
        KnownClassDescriptor.createClass(Name.identifier("Ref"), kotlinJvmInternalPackage, listOf(builtIns.anyType))

    private class PrimitiveRefDescriptorsProvider(type: KotlinType, refClass: ClassDescriptor) {
        val refType: KotlinType = refClass.defaultType

        val refConstructor: ClassConstructorDescriptor =
            ClassConstructorDescriptorImpl.create(refClass, Annotations.EMPTY, true, SourceElement.NO_SOURCE).apply {
                initialize(emptyList(), Visibilities.PUBLIC, emptyList())
                returnType = refType
            }

        val refConstructorSymbol = IrConstructorSymbolImpl(refConstructor)

        val elementField: PropertyDescriptor =
            PropertyDescriptorImpl.create(
                refClass, Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC, true,
                Name.identifier("element"), CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE,
                /* lateInit = */ false, /* isConst = */ false, /* isExpect = */ false, /* isActual = */ false,
                /* isExternal = */ false, /* isDelegated = */ false
            ).initialize(type, dispatchReceiverParameter = refClass.thisAsReceiverParameter)

        val elementFieldSymbol = IrFieldSymbolImpl(elementField)
        val elementFieldDeclaration =
            IrFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SHARED_VARIABLE_ORIGIN, elementFieldSymbol, type.toIrType()!!)
    }

    private val primitiveRefDescriptorProviders: Map<PrimitiveType, PrimitiveRefDescriptorsProvider> =
        PrimitiveType.values().associate {
            val type = builtIns.getPrimitiveKotlinType(it)

            val refClassName = Name.identifier(it.typeName.asString() + "Ref")
            val refClass = KnownClassDescriptor.createClass(refClassName, refNamespaceClass, listOf(builtIns.anyType))

            it to PrimitiveRefDescriptorsProvider(type, refClass)
        }

    private inner class ObjectRefDescriptorsProvider {
        val genericRefClass: ClassDescriptor =
            KnownClassDescriptor.createClassWithTypeParameters(
                Name.identifier("ObjectRef"), refNamespaceClass, listOf(builtIns.anyType), listOf(Name.identifier("T"))
            )

        val genericRefConstructor: ClassConstructorDescriptor =
            ClassConstructorDescriptorImpl.create(genericRefClass, Annotations.EMPTY, true, SourceElement.NO_SOURCE).apply {
                initialize(emptyList(), Visibilities.PUBLIC)
                val typeParameter = typeParameters[0]
                val typeParameterType = KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
                    Annotations.EMPTY,
                    typeParameter.typeConstructor,
                    listOf(),
                    false,
                    MemberScope.Empty
                )
                returnType = KotlinTypeFactory.simpleNotNullType(
                    Annotations.EMPTY,
                    genericRefClass,
                    listOf(TypeProjectionImpl(Variance.INVARIANT, typeParameterType))
                )
            }

        val constructorTypeParameter: TypeParameterDescriptor =
            genericRefConstructor.typeParameters[0]

        fun getSubstitutedRefConstructor(valueType: KotlinType): ClassConstructorDescriptor =
            genericRefConstructor.substitute(
                TypeSubstitutor.create(
                    mapOf(constructorTypeParameter.typeConstructor to TypeProjectionImpl(Variance.INVARIANT, valueType))
                )
            )!!

        val genericElementField: PropertyDescriptor =
            PropertyDescriptorImpl.create(
                genericRefClass, Annotations.EMPTY, Modality.FINAL, Visibilities.PUBLIC, true,
                Name.identifier("element"), CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE,
                /* lateInit = */ false, /* isConst = */ false, /* isExpect = */ false, /* isActual = */ false,
                /* isExternal = */ false, /* isDelegated = */ false
            ).initialize(
                type = builtIns.anyType,
                dispatchReceiverParameter = genericRefClass.thisAsReceiverParameter
            )

        val genericElementFieldSymbol = IrFieldSymbolImpl(genericElementField)
        val elementFieldDeclaration =
            IrFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SHARED_VARIABLE_ORIGIN, genericElementFieldSymbol, irBuiltIns.anyType)


        fun getRefType(valueType: KotlinType) =
            KotlinTypeFactory.simpleNotNullType(
                Annotations.EMPTY,
                genericRefClass,
                listOf(TypeProjectionImpl(Variance.INVARIANT, valueType))
            )
    }

    private val objectRefDescriptorsProvider = ObjectRefDescriptorsProvider()

    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val variableDescriptor = originalDeclaration.descriptor
        val sharedVariableDescriptor = LocalVariableDescriptor(
            variableDescriptor.containingDeclaration, variableDescriptor.annotations, variableDescriptor.name,
            getSharedVariableType(variableDescriptor.type),
            false, false, variableDescriptor.isLateInit, variableDescriptor.source
        )
        val sharedVariableSymbol = IrVariableSymbolImpl(sharedVariableDescriptor)

        val valueType = originalDeclaration.descriptor.type
        val primitiveRefDescriptorsProvider = primitiveRefDescriptorProviders[getPrimitiveType(valueType)]

        val refConstructor =
            primitiveRefDescriptorsProvider?.refConstructor ?: objectRefDescriptorsProvider.getSubstitutedRefConstructor(valueType)

        val refConstructorSymbol =
            primitiveRefDescriptorsProvider?.refConstructorSymbol ?: createFunctionSymbol(refConstructor) as IrConstructorSymbol

        val refConstructorDeclaration = if (refConstructorSymbol.isBound) refConstructorSymbol.owner else
            IrConstructorImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SHARED_VARIABLE_ORIGIN, refConstructorSymbol)

        val refConstructorTypeArguments =
            if (primitiveRefDescriptorsProvider != null) null
            else mapOf(objectRefDescriptorsProvider.constructorTypeParameter to valueType)


        val refConstructorCall = IrCallImpl(
            originalDeclaration.startOffset, originalDeclaration.endOffset,
            refConstructor.constructedClass.defaultType.toIrType()!!,
            refConstructorSymbol, refConstructor,
            refConstructorTypeArguments?.size ?: 0
        )
        return IrVariableImpl(
            originalDeclaration.startOffset, originalDeclaration.endOffset, originalDeclaration.origin,
            sharedVariableSymbol, sharedVariableDescriptor.type.toIrType()!!
        ).apply { initializer = refConstructorCall }
    }

    override fun defineSharedValue(
        originalDeclaration: IrVariable,
        sharedVariableDeclaration: IrVariable
    ): IrStatement {
        val initializer = originalDeclaration.initializer ?: return sharedVariableDeclaration

        val valueType = originalDeclaration.descriptor.type
        val primitiveRefDescriptorsProvider = primitiveRefDescriptorProviders[getPrimitiveType(valueType)]

        val elementPropertySymbol =
            primitiveRefDescriptorsProvider?.elementFieldSymbol ?: objectRefDescriptorsProvider.genericElementFieldSymbol

        val sharedVariableInitialization = IrSetFieldImpl(
            initializer.startOffset, initializer.endOffset,
            elementPropertySymbol,
            IrGetValueImpl(initializer.startOffset, initializer.endOffset, sharedVariableDeclaration.symbol),
            initializer,
            originalDeclaration.type
        )

        return IrCompositeImpl(
            originalDeclaration.startOffset, originalDeclaration.endOffset, irBuiltIns.unitType, null,
            listOf(sharedVariableDeclaration, sharedVariableInitialization)
        )
    }

    private fun getElementFieldSymbol(valueType: KotlinType): IrFieldSymbol {
        val primitiveRefDescriptorsProvider = primitiveRefDescriptorProviders[getPrimitiveType(valueType)]

        return primitiveRefDescriptorsProvider?.elementFieldSymbol ?: objectRefDescriptorsProvider.genericElementFieldSymbol
    }

    override fun getSharedValue(sharedVariableSymbol: IrVariableSymbol, originalGet: IrGetValue): IrExpression =
        IrGetFieldImpl(
            originalGet.startOffset, originalGet.endOffset,
            getElementFieldSymbol(originalGet.descriptor.type),
            originalGet.type,
            IrGetValueImpl(originalGet.startOffset, originalGet.endOffset, sharedVariableSymbol),
            originalGet.origin
        )

    override fun setSharedValue(sharedVariableSymbol: IrVariableSymbol, originalSet: IrSetVariable): IrExpression =
        IrSetFieldImpl(
            originalSet.startOffset, originalSet.endOffset,
            getElementFieldSymbol(originalSet.descriptor.type),
            IrGetValueImpl(originalSet.startOffset, originalSet.endOffset, sharedVariableSymbol),
            originalSet.value,
            originalSet.type,
            originalSet.origin
        )

    private fun getSharedVariableType(valueType: KotlinType): KotlinType =
        primitiveRefDescriptorProviders[getPrimitiveType(valueType)]?.refType ?: objectRefDescriptorsProvider.getRefType(valueType)

    private fun getPrimitiveType(type: KotlinType): PrimitiveType? =
        when {
            KotlinBuiltIns.isBoolean(type) -> PrimitiveType.BOOLEAN
            KotlinBuiltIns.isChar(type) -> PrimitiveType.CHAR
            KotlinBuiltIns.isByte(type) -> PrimitiveType.BYTE
            KotlinBuiltIns.isShort(type) -> PrimitiveType.SHORT
            KotlinBuiltIns.isInt(type) -> PrimitiveType.INT
            KotlinBuiltIns.isLong(type) -> PrimitiveType.LONG
            KotlinBuiltIns.isFloat(type) -> PrimitiveType.FLOAT
            KotlinBuiltIns.isDouble(type) -> PrimitiveType.DOUBLE
            else -> null
        }

}
