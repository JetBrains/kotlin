/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*

private val SHARED_VARIABLE_ORIGIN = object : IrDeclarationOriginImpl("SHARED_VARIABLE_ORIGIN") {}
private val SHARED_VARIABLE_CONSTRUCTOR_CALL_ORIGIN = object : IrStatementOriginImpl("SHARED_VARIABLE_CONSTRUCTOR_CALL") {}

class JvmSharedVariablesManager(
    module: ModuleDescriptor,
    val builtIns: KotlinBuiltIns,
    val irBuiltIns: IrBuiltIns
) : SharedVariablesManager {
    private val jvmInternalPackage = IrExternalPackageFragmentImpl(
        IrExternalPackageFragmentSymbolImpl(
            EmptyPackageFragmentDescriptor(module, FqName("kotlin.jvm.internal"))
        )
    )

    private val refNamespaceClass = buildClass {
        name = Name.identifier("Ref")
    }.apply {
        parent = jvmInternalPackage
        jvmInternalPackage.addChild(this)
        superTypes.add(irBuiltIns.anyType)
    }

    private abstract class RefProvider {
        abstract val elementType: IrType
        abstract val refClass: IrClass
        abstract fun getRefType(valueType: IrType): IrSimpleType

        // Have to initialize fields lazily in order to refer to refClass.
        val refConstructor: IrConstructor by lazy {
            buildConstructor {
                origin = SHARED_VARIABLE_ORIGIN
                returnType = refClass.defaultType
            }.apply {
                parent = refClass
                refClass.addMember(this)
            }
        }

        val elementField: IrField by lazy {
            buildField {
                origin = SHARED_VARIABLE_ORIGIN
                name = Name.identifier("element")
                type = elementType
            }.apply {
                parent = refClass
                refClass.addMember(this)
            }
        }
    }

    private inner class PrimitiveRefProvider(primitiveType: PrimitiveType) : RefProvider() {
        override val elementType = builtIns.getPrimitiveKotlinType(primitiveType).toIrType()!!

        override val refClass = buildClass {
            origin = SHARED_VARIABLE_ORIGIN
            name = Name.identifier(primitiveType.typeName.asString() + "Ref")
        }.apply {
            parent = refNamespaceClass
            refNamespaceClass.addMember(this)
            superTypes.add(irBuiltIns.anyType)
            thisReceiver = buildValueParameter {
                type = IrSimpleTypeImpl(symbol, hasQuestionMark = false, arguments = emptyList(), annotations = emptyList())
                name = Name.identifier("$this")
            }.also {
                it.parent = this
            }
        }

        override fun getRefType(valueType: IrType) = refClass.defaultType
    }

    private val primitiveRefProviders = PrimitiveType.values().associate { primitiveType ->
        primitiveType to PrimitiveRefProvider(primitiveType)
    }

    private val objectRefProvider = object : RefProvider() {
        override val refClass = buildClass {
            origin = SHARED_VARIABLE_ORIGIN
            name = Name.identifier("ObjectRef")
        }.apply {
            val irClass = this
            typeParameters.add(
                IrTypeParameterImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    SHARED_VARIABLE_ORIGIN,
                    IrTypeParameterSymbolImpl(WrappedTypeParameterDescriptor()),
                    Name.identifier("T"),
                    index = 0,
                    variance = Variance.INVARIANT,
                    isReified = false
                ).apply {
                    (descriptor as WrappedTypeParameterDescriptor).bind(this)
                    parent = irClass
                    superTypes.add(irBuiltIns.anyNType)
                }
            )
            parent = refNamespaceClass
            refNamespaceClass.addMember(this)
            superTypes.add(irBuiltIns.anyType)
            thisReceiver = buildValueParameter {
                type = IrSimpleTypeImpl(
                    symbol,
                    hasQuestionMark = false,
                    arguments = listOf(
                        makeTypeProjection(typeParameters[0].defaultType, Variance.INVARIANT)
                    ),
                    annotations = emptyList()
                )
                name = Name.identifier("$this")
            }.also {
                it.parent = this
            }
        }

        override val elementType = refClass.typeParameters[0].defaultType

        override fun getRefType(valueType: IrType) = refClass.typeWith(listOf(valueType))
    }

    private fun getProvider(valueType: IrType) = primitiveRefProviders[getPrimitiveType(valueType)] ?: objectRefProvider

    private fun getElementFieldSymbol(valueType: IrType): IrFieldSymbol {
        return getProvider(valueType).elementField.symbol
    }

    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val valueType = originalDeclaration.type
        val provider = getProvider(valueType)
        val refType = provider.getRefType(valueType)
        val refConstructor = provider.refConstructor
        val typeArgumentsCount = refConstructor.parentAsClass.typeParameters.count()

        val refConstructorCall = IrCallImpl(
            originalDeclaration.startOffset, originalDeclaration.endOffset,
            refType,
            refConstructor.symbol, refConstructor.descriptor,
            typeArgumentsCount = typeArgumentsCount,
            origin = SHARED_VARIABLE_CONSTRUCTOR_CALL_ORIGIN
        ).apply {
            refConstructor.parentAsClass.typeParameters.mapIndexed { i, param ->
                putTypeArgument(i, valueType)
            }
        }

        return IrVariableImpl(
            originalDeclaration.startOffset, originalDeclaration.endOffset, originalDeclaration.origin,
            IrVariableSymbolImpl(WrappedVariableDescriptor()),
            originalDeclaration.name,
            refType,
            originalDeclaration.isVar,
            originalDeclaration.isConst,
            isLateinit = false
        ).apply {
            (descriptor as WrappedVariableDescriptor).bind(this)
            initializer = refConstructorCall
        }
    }

    override fun defineSharedValue(
        originalDeclaration: IrVariable,
        sharedVariableDeclaration: IrVariable
    ): IrStatement {
        val initializer = originalDeclaration.initializer ?: return sharedVariableDeclaration

        val valueType = originalDeclaration.type

        val sharedVariableInitialization = IrSetFieldImpl(
            initializer.startOffset, initializer.endOffset,
            getElementFieldSymbol(valueType),
            IrGetValueImpl(initializer.startOffset, initializer.endOffset, sharedVariableDeclaration.symbol),
            initializer,
            valueType
        )

        return IrCompositeImpl(
            originalDeclaration.startOffset, originalDeclaration.endOffset, irBuiltIns.unitType, null,
            listOf(sharedVariableDeclaration, sharedVariableInitialization)
        )
    }

    override fun getSharedValue(sharedVariableSymbol: IrVariableSymbol, originalGet: IrGetValue): IrExpression =
        IrTypeOperatorCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            originalGet.type,
            IrTypeOperator.IMPLICIT_CAST,
            originalGet.type,
            (originalGet.type as IrSimpleType).classifier,
            IrGetFieldImpl(
                originalGet.startOffset, originalGet.endOffset,
                getElementFieldSymbol(originalGet.symbol.owner.type),
                originalGet.type,
                IrGetValueImpl(originalGet.startOffset, originalGet.endOffset, sharedVariableSymbol),
                originalGet.origin
            )
        )

    override fun setSharedValue(sharedVariableSymbol: IrVariableSymbol, originalSet: IrSetVariable): IrExpression =
        IrSetFieldImpl(
            originalSet.startOffset, originalSet.endOffset,
            getElementFieldSymbol(originalSet.symbol.owner.type),
            IrGetValueImpl(originalSet.startOffset, originalSet.endOffset, sharedVariableSymbol),
            originalSet.value,
            originalSet.type,
            originalSet.origin
        )

    private fun getPrimitiveType(type: IrType): PrimitiveType? {
        val kType = type.toKotlinType()
        return when {
            KotlinBuiltIns.isBoolean(kType) -> PrimitiveType.BOOLEAN
            KotlinBuiltIns.isChar(kType) -> PrimitiveType.CHAR
            KotlinBuiltIns.isByte(kType) -> PrimitiveType.BYTE
            KotlinBuiltIns.isShort(kType) -> PrimitiveType.SHORT
            KotlinBuiltIns.isInt(kType) -> PrimitiveType.INT
            KotlinBuiltIns.isLong(kType) -> PrimitiveType.LONG
            KotlinBuiltIns.isFloat(kType) -> PrimitiveType.FLOAT
            KotlinBuiltIns.isDouble(kType) -> PrimitiveType.DOUBLE
            else -> null
        }
    }
}
