/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ConstantValueGenerator(
    private val moduleDescriptor: ModuleDescriptor,
    private val symbolTable: ReferenceSymbolTable
) {

    lateinit var typeTranslator: TypeTranslator

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
            is UIntValue -> IrConstImpl.int(startOffset, endOffset, constantType, constantValue.value)
            is NullValue -> IrConstImpl.constNull(startOffset, endOffset, constantType)
            is BooleanValue -> IrConstImpl.boolean(startOffset, endOffset, constantType, constantValue.value)
            is LongValue -> IrConstImpl.long(startOffset, endOffset, constantType, constantValue.value)
            is ULongValue -> IrConstImpl.long(startOffset, endOffset, constantType, constantValue.value)
            is DoubleValue -> IrConstImpl.double(startOffset, endOffset, constantType, constantValue.value)
            is FloatValue -> IrConstImpl.float(startOffset, endOffset, constantType, constantValue.value)
            is CharValue -> IrConstImpl.char(startOffset, endOffset, constantType, constantValue.value)
            is ByteValue -> IrConstImpl.byte(startOffset, endOffset, constantType, constantValue.value)
            is UByteValue -> IrConstImpl.byte(startOffset, endOffset, constantType, constantValue.value)
            is ShortValue -> IrConstImpl.short(startOffset, endOffset, constantType, constantValue.value)
            is UShortValue -> IrConstImpl.short(startOffset, endOffset, constantType, constantValue.value)

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

            is AnnotationValue -> generateAnnotationConstructorCall(constantValue.value)

            is KClassValue -> {
                val classifierKtType = constantValue.getArgumentType(moduleDescriptor)
                val classifierDescriptor = classifierKtType.constructor.declarationDescriptor
                        ?: throw AssertionError("Unexpected KClassValue: $classifierKtType")

                IrClassReferenceImpl(
                    startOffset, endOffset,
                    constantValue.getType(moduleDescriptor).toIrType(),
                    classifierDescriptor.defaultType.toIrType().classifierOrFail,
                    classifierKtType.toIrType()
                )
            }

            else -> TODO("Unexpected constant value: ${constantValue.javaClass.simpleName} $constantValue")
        }
    }

    fun generateAnnotationConstructorCall(annotationDescriptor: AnnotationDescriptor): IrCall {
        val annotationType = annotationDescriptor.type
        val annotationClassDescriptor = annotationType.constructor.declarationDescriptor as? ClassDescriptor
                ?: throw AssertionError("No declaration descriptor for annotation $annotationDescriptor")

        assert(DescriptorUtils.isAnnotationClass(annotationClassDescriptor)) {
            "Annotation class expected: $annotationClassDescriptor"
        }

        val primaryConstructorDescriptor = annotationClassDescriptor.unsubstitutedPrimaryConstructor
                ?: annotationClassDescriptor.constructors.singleOrNull()
                ?: throw AssertionError("No constructor for annotation class $annotationClassDescriptor")
        val primaryConstructorSymbol = symbolTable.referenceConstructor(primaryConstructorDescriptor)

        val psi = annotationDescriptor.source.safeAs<PsiSourceElement>()?.psi
        val startOffset = psi?.startOffset ?: UNDEFINED_OFFSET
        val endOffset = psi?.startOffset ?: UNDEFINED_OFFSET

        val irCall = IrCallImpl(
            startOffset, endOffset,
            annotationType.toIrType(),
            primaryConstructorSymbol, primaryConstructorDescriptor,
            typeArgumentsCount = 0
        )

        for (valueParameter in primaryConstructorDescriptor.valueParameters) {
            val argumentIndex = valueParameter.index
            val argumentValue = annotationDescriptor.allValueArguments[valueParameter.name] ?: continue
            val irArgument = generateConstantValueAsExpression(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                argumentValue,
                valueParameter.varargElementType
            )
            irCall.putValueArgument(argumentIndex, irArgument)
        }

        return irCall
    }

    private fun KotlinType.getArrayElementType() = builtIns.getArrayElementType(this)
}