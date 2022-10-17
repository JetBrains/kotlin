/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.error.ErrorClassDescriptor
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.builtIns

abstract class ConstantValueGenerator(
    private val moduleDescriptor: ModuleDescriptor,
    private val symbolTable: ReferenceSymbolTable,
    private val typeTranslator: TypeTranslator,
    private val allowErrorTypeInAnnotations: Boolean,
) {
    protected abstract fun extractAnnotationOffsets(annotationDescriptor: AnnotationDescriptor): Pair<Int, Int>

    protected abstract fun extractAnnotationParameterOffsets(annotationDescriptor: AnnotationDescriptor, argumentName: Name): Pair<Int, Int>

    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun generateConstantValueAsExpression(
        startOffset: Int,
        endOffset: Int,
        constantValue: ConstantValue<*>,
    ): IrExpression =
        // Assertion is safe here because annotation calls and class literals are not allowed in constant initializers
        generateConstantOrAnnotationValueAsExpression(startOffset, endOffset, constantValue, null, null)!!

    /**
     * @return null if the constant value is an unresolved annotation or an unresolved class literal
     */
    fun generateAnnotationValueAsExpression(
        startOffset: Int,
        endOffset: Int,
        constantValue: ConstantValue<*>,
        valueParameter: ValueParameterDescriptor,
    ): IrExpression? =
        generateConstantOrAnnotationValueAsExpression(
            startOffset, endOffset, constantValue, valueParameter.type, valueParameter.varargElementType
        )

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun generateConstantOrAnnotationValueAsExpression(
        startOffset: Int,
        endOffset: Int,
        constantValue: ConstantValue<*>,
        expectedType: KotlinType?,
        expectedArrayElementType: KotlinType?
    ): IrExpression? {
        val constantValueType = constantValue.getType(moduleDescriptor)
        val constantKtType = expectedType ?: constantValueType
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
                //  TODO: in `spreadOperatorInAnnotationArguments`, `@A(*arrayOf("a"), *arrayOf("b"))` is incorrectly
                //    translated into `A(xs = [['a'], ['b']])` instead of `A(xs = ['a', 'b'])`. Not using `expectedType`
                //    here masks that.
                val arrayElementType = expectedArrayElementType ?: constantValueType.getArrayElementType()
                IrVarargImpl(
                    startOffset, endOffset,
                    constantType,
                    arrayElementType.toIrType(),
                    constantValue.value.mapNotNull {
                        // For annotation arguments, the type of every subexpression can be inferred from the type of the parameter;
                        // for arbitrary constants, we should always take the type inferred by the frontend.
                        val newExpectedType = arrayElementType.takeIf { expectedType != null }
                        generateConstantOrAnnotationValueAsExpression(startOffset, endOffset, it, newExpectedType, null)
                    }
                )
            }

            is EnumValue -> {
                //  TODO: in `annotationWithKotlinProperty`, `@Foo(KotlinClass.FOO_INT)` is parsed as if `KotlinClass.FOO_INT`
                //    is an EnumValue when it's a read of a `const val` with an Int type. Not using `expectedType` somewhat masks
                //    that - we silently fail to translate the argument because `enumEntryDescriptor` is an error class.
                val enumEntryDescriptor =
                    constantValueType.memberScope.getContributedClassifier(constantValue.enumEntryName, NoLookupLocation.FROM_BACKEND)
                        ?: throw AssertionError("No such enum entry ${constantValue.enumEntryName} in $constantType")
                if (enumEntryDescriptor !is ClassDescriptor) {
                    throw AssertionError("Enum entry $enumEntryDescriptor should be a ClassDescriptor")
                }
                if (!DescriptorUtils.isEnumEntry(enumEntryDescriptor)) {
                    // Error class descriptor for an unresolved entry.
                    // TODO this `null` may actually reach codegen if the annotation is on an interface member's default implementation,
                    //      as any bridge generated in an implementation of that interface will have a copy of the annotation. See
                    //      `missingEnumReferencedInAnnotationArgumentIr` in `testData/compileKotlinAgainstCustomBinaries`: replace
                    //      `open class B` with `interface B` and watch things break. (`KClassValue` below likely has a similar problem.)
                    return null
                }
                IrGetEnumValueImpl(
                    startOffset, endOffset,
                    constantType,
                    symbolTable.referenceEnumEntry(enumEntryDescriptor)
                )
            }

            is AnnotationValue -> generateAnnotationConstructorCall(constantValue.value, constantKtType)

            is KClassValue -> {
                val classifierKtType = constantValue.getArgumentType(moduleDescriptor)
                if (classifierKtType.isError) null
                else {
                    val classifierDescriptor = classifierKtType.constructor.declarationDescriptor
                        ?: throw AssertionError("Unexpected KClassValue: $classifierKtType")

                    IrClassReferenceImpl(
                        startOffset, endOffset,
                        constantValue.getType(moduleDescriptor).toIrType(),
                        symbolTable.referenceClassifier(classifierDescriptor),
                        classifierKtType.toIrType()
                    )
                }
            }

            is ErrorValue -> null

            else -> TODO("Unexpected constant value: ${constantValue.javaClass.simpleName} $constantValue")
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun generateAnnotationConstructorCall(annotationDescriptor: AnnotationDescriptor, realType: KotlinType? = null): IrConstructorCall? {
        val annotationType = realType ?: annotationDescriptor.type
        val annotationClassDescriptor = annotationType.constructor.declarationDescriptor as? ClassDescriptor ?: return null

        when (annotationClassDescriptor) {
            is NotFoundClasses.MockClassDescriptor -> return null
            is ErrorClassDescriptor -> if (!allowErrorTypeInAnnotations) return null
            else -> if (!DescriptorUtils.isAnnotationClass(annotationClassDescriptor)) return null
        }

        val primaryConstructorDescriptor = annotationClassDescriptor.unsubstitutedPrimaryConstructor
            ?: annotationClassDescriptor.constructors.singleOrNull()
            ?: throw AssertionError("No constructor for annotation class $annotationClassDescriptor")
        val primaryConstructorSymbol = symbolTable.referenceConstructor(primaryConstructorDescriptor)

        val (startOffset, endOffset) = extractAnnotationOffsets(annotationDescriptor)

        val irCall = IrConstructorCallImpl(
            startOffset, endOffset,
            annotationType.toIrType(),
            primaryConstructorSymbol,
            valueArgumentsCount = primaryConstructorDescriptor.valueParameters.size,
            typeArgumentsCount = annotationClassDescriptor.declaredTypeParameters.size,
            constructorTypeArgumentsCount = 0,
            source = annotationDescriptor.source
        )

        val substitutor = TypeConstructorSubstitution.create(annotationType).buildSubstitutor()
        val substitutedConstructor = primaryConstructorDescriptor.substitute(substitutor) ?: error("Cannot substitute constructor")

        val typeArguments = annotationType.arguments
        assert(typeArguments.size == annotationClassDescriptor.declaredTypeParameters.size)

        for (i in typeArguments.indices) {
            val typeArgument = typeArguments[i]
            irCall.putTypeArgument(i, typeArgument.type.toIrType())
        }

        for (valueParameter in substitutedConstructor.valueParameters) {
            val argumentIndex = valueParameter.index
            val argumentValue = annotationDescriptor.allValueArguments[valueParameter.name] ?: continue
            val adjustedValue = adjustAnnotationArgumentValue(argumentValue, valueParameter)
            val (parameterStartOffset, parameterEndOffset) = extractAnnotationParameterOffsets(annotationDescriptor, valueParameter.name)
            val irArgument = generateAnnotationValueAsExpression(parameterStartOffset, parameterEndOffset, adjustedValue, valueParameter)
            irCall.putValueArgument(argumentIndex, irArgument)
        }

        return irCall
    }

    private fun adjustAnnotationArgumentValue(value: ConstantValue<*>, parameter: ValueParameterDescriptor): ConstantValue<*> {
        // In Java source code, annotation argument for an array-typed parameter can be a single value instead of an array.
        // In that case, wrap it into an array manually. Ideally, this should be fixed in the code which loads Java annotation arguments,
        // but it would require resolving the annotation class on each request of an annotation argument.
        if (KotlinBuiltIns.isArrayOrPrimitiveArray(parameter.type) && value !is ArrayValue) {
            return ArrayValue(listOf(value)) { parameter.type }
        }
        return value
    }

    private fun KotlinType.getArrayElementType() = builtIns.getArrayElementType(this)
}
