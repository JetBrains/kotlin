/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ConstantValueGenerator(
    private val moduleDescriptor: ModuleDescriptor,
    private val symbolTable: SymbolTable,
    private val annotationGenerator: AnnotationGenerator?,
    private val scopedTypeParameterResolver: ScopedTypeParametersResolver?
) {

    private val typeTranslator = TypeTranslator(moduleDescriptor, symbolTable)

    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun generateConstantValueAsExpression(
        startOffset: Int,
        endOffset: Int,
        constantValue: ConstantValue<*>,
        varargElementType: KotlinType? = null
    ): IrExpression {
        val constantKtType = constantValue.getType(moduleDescriptor)
        val constantType = constantKtType.toIrType()

        return when (constantValue) {
            is StringValue -> IrConstImpl.string(startOffset, endOffset, constantType, constantValue.value)
            is IntValue -> IrConstImpl.int(startOffset, endOffset, constantType, constantValue.value)
            is NullValue -> IrConstImpl.constNull(startOffset, endOffset, constantType)
            is BooleanValue -> IrConstImpl.boolean(startOffset, endOffset, constantType, constantValue.value)
            is LongValue -> IrConstImpl.long(startOffset, endOffset, constantType, constantValue.value)
            is DoubleValue -> IrConstImpl.double(startOffset, endOffset, constantType, constantValue.value)
            is FloatValue -> IrConstImpl.float(startOffset, endOffset, constantType, constantValue.value)
            is CharValue -> IrConstImpl.char(startOffset, endOffset, constantType, constantValue.value)
            is ByteValue -> IrConstImpl.byte(startOffset, endOffset, constantType, constantValue.value)
            is ShortValue -> IrConstImpl.short(startOffset, endOffset, constantType, constantValue.value)

            is ArrayValue -> {
                val arrayElementType = varargElementType ?: constantKtType.getArrayElementType()
                IrVarargImpl(
                    startOffset, endOffset,
                    constantType,
                    arrayElementType.toIrType(),
                    constantValue.value.map {
                        generateConstantValueAsExpression(startOffset, endOffset, it, null)
                    }
                )
            }

            is EnumValue -> {
                val enumEntryDescriptor =
                    constantKtType.memberScope.getContributedClassifier(constantValue.enumEntryName, NoLookupLocation.FROM_BACKEND)
                            ?: throw AssertionError("No such enum entry ${constantValue.enumEntryName} in $constantType")
                if (enumEntryDescriptor !is ClassDescriptor) {
                    throw AssertionError("Enum entry $enumEntryDescriptor should be a ClassDescriptor")
                }
                IrGetEnumValueImpl(
                    startOffset, endOffset,
                    constantType,
                    symbolTable.referenceEnumEntry(enumEntryDescriptor)
                )
            }

            is AnnotationValue -> {
                if (annotationGenerator == null) throw AssertionError("Unexpected AnnotationValue: $constantValue")
                annotationGenerator.generateAnnotationConstructorCall(constantValue.value)
            }

            is KClassValue -> {
                val classifierKtType = constantValue.value
                val classifierDescriptor = classifierKtType.constructor.declarationDescriptor
                        ?: throw AssertionError("Unexpected KClassValue: $classifierKtType")

                val typeParameterSymbol = classifierDescriptor.safeAs<TypeParameterDescriptor>()?.let {
                    scopedTypeParameterResolver?.resolveScopedTypeParameter(it)
                }

                IrClassReferenceImpl(
                    startOffset, endOffset,
                    constantValue.getType(moduleDescriptor).toIrType(),
                    typeParameterSymbol ?: symbolTable.referenceClassifier(classifierDescriptor),
                    classifierKtType.toIrType()
                )
            }

            else -> TODO("Unexpected constant value: ${constantValue.javaClass.simpleName} $constantValue")
        }
    }

    private fun KotlinType.getArrayElementType() = builtIns.getArrayElementType(this)
}