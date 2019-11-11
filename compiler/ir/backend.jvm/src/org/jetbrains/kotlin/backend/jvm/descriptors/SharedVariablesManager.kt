/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.WrappedTypeParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

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

    private inner class PrimitiveRefProvider(override val elementType: IrType) : RefProvider() {
        override val refClass = buildClass {
            origin = SHARED_VARIABLE_ORIGIN
            name = Name.identifier(elementType.classOrNull!!.owner.name.asString() + "Ref")
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

    private val primitiveRefProviders = irBuiltIns.primitiveIrTypes.associate { primitiveType ->
        primitiveType.classifierOrFail to PrimitiveRefProvider(primitiveType)
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

        override fun getRefType(valueType: IrType) = refClass.typeWith(valueType)
    }

    private fun getProvider(valueType: IrType): RefProvider =
        if (valueType.isPrimitiveType())
            primitiveRefProviders.getValue(valueType.classifierOrFail)
        else
            objectRefProvider

    private fun getElementFieldSymbol(valueType: IrType): IrFieldSymbol {
        return getProvider(valueType).elementField.symbol
    }

    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val valueType = originalDeclaration.type
        val provider = getProvider(valueType)
        val refType = provider.getRefType(valueType)
        val refConstructor = provider.refConstructor

        val refConstructorCall = IrConstructorCallImpl.fromSymbolOwner(
            refType,
            refConstructor.symbol,
            SHARED_VARIABLE_CONSTRUCTOR_CALL_ORIGIN
        ).apply {
            List(refConstructor.parentAsClass.typeParameters.size) { i ->
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
            parent = originalDeclaration.parent
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
            irBuiltIns.unitType
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
}
