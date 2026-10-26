/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.buildUnaryArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationEntryStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class StubBasedAnnotationDeserializer(private val session: FirSession) {
    companion object {
        fun getAnnotationClassId(ktAnnotation: KtAnnotationEntry): ClassId {
            val userType = ktAnnotation.calleeExpression?.typeReference?.typeElement
            requireWithAttachment(
                userType is KtUserType,
                { "${KtTypeElement::class.simpleName} should be ${KtUserType::class.simpleName}" },
            ) {
                withPsiEntry("annotationEntry", ktAnnotation)
            }

            return userType.classId()
        }

        val RECEIVER_ANNOTATIONS_FILTER: (AnnotationUseSiteTarget?) -> Boolean = {
            it == AnnotationUseSiteTarget.RECEIVER
        }

        /**
         * A folded constructor parameter carries the annotations of the property it declares as well as its own,
         * so each of these filters picks the declaration an annotation was written on.
         */
        val VALUE_PARAMETER_ANNOTATIONS_FILTER: (AnnotationUseSiteTarget?) -> Boolean = {
            it == null || it == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
        }

        val PROPERTY_ANNOTATIONS_FILTER: (AnnotationUseSiteTarget?) -> Boolean = {
            it == AnnotationUseSiteTarget.PROPERTY
        }

        val GETTER_ANNOTATIONS_FILTER: (AnnotationUseSiteTarget?) -> Boolean = {
            it == AnnotationUseSiteTarget.PROPERTY_GETTER
        }

        val SETTER_ANNOTATIONS_FILTER: (AnnotationUseSiteTarget?) -> Boolean = {
            it == AnnotationUseSiteTarget.PROPERTY_SETTER
        }

        val BACKING_FIELD_ANNOTATIONS_FILTER: (AnnotationUseSiteTarget?) -> Boolean = {
            it == AnnotationUseSiteTarget.FIELD || it == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
        }

        val TYPE_ANNOTATIONS_FILTER: (AnnotationUseSiteTarget?) -> Boolean = {
            it == null
        }
    }

    fun loadAnnotations(
        ktAnnotated: KtAnnotated,
        preserveUseSiteTarget: Boolean = true,
        useSiteTargetFilter: ((AnnotationUseSiteTarget?) -> Boolean)? = null,
    ): List<FirAnnotation> {
        val annotations = ktAnnotated.annotationEntries
        if (annotations.isEmpty()) {
            return emptyList()
        }

        return annotations.mapNotNull {
            deserializeAnnotation(
                ktAnnotation = it,
                preserveUseSiteTarget = preserveUseSiteTarget,
                useSiteTargetFilter = useSiteTargetFilter,
            )
        }
    }

    fun loadConstant(property: KtProperty, type: ConeKotlinType, isFromAnnotation: Boolean): FirExpression? {
        // Default values for annotation properties have constant as a workaround for KT-58137 (ee30cc04ee810fcdd719085af3a0e0c1995a73ee)
        if (!property.hasModifier(KtTokens.CONST_KEYWORD) && !isFromAnnotation) return null
        val propertyStub: KotlinPropertyStubImpl = property.compiledStub
        val constantValue = propertyStub.constantInitializer ?: return null
        return resolveConstant(property, constantValue, type)
    }

    /**
     * Builds the expression [constantValue] stands for, as it is written at [sourceElement].
     */
    fun resolveConstant(sourceElement: PsiElement, constantValue: ConstantValue<*>, type: ConeKotlinType): FirExpression {
        val resultValue = when {
            !type.isUnsignedType -> constantValue
            type.isUByte -> UByteValue((constantValue.value as Number).toByte())
            type.isUShort -> UShortValue((constantValue.value as Number).toShort())
            type.isUInt -> UIntValue((constantValue.value as Number).toInt())
            type.isULong -> ULongValue((constantValue.value as Number).toLong())
            else -> constantValue
        }

        return resolveValue(sourceElement, resultValue)
    }

    private fun deserializeAnnotation(
        ktAnnotation: KtAnnotationEntry,
        preserveUseSiteTarget: Boolean,
        useSiteTargetFilter: ((AnnotationUseSiteTarget?) -> Boolean)? = null,
    ): FirAnnotation? {
        val useSiteTarget = ktAnnotation.useSiteTarget?.getAnnotationUseSiteTarget()
        if (useSiteTargetFilter?.invoke(useSiteTarget) == false) {
            return null
        }

        val annotationStub: KotlinAnnotationEntryStubImpl = ktAnnotation.compiledStub
        val valueArguments = annotationStub.valueArguments

        return deserializeAnnotation(
            ktAnnotation = ktAnnotation,
            classId = getAnnotationClassId(ktAnnotation),
            valueArguments = valueArguments,
            useSiteTarget = if (preserveUseSiteTarget) useSiteTarget else null,
        )
    }

    private fun deserializeAnnotation(
        ktAnnotation: PsiElement,
        classId: ClassId,
        valueArguments: Map<Name, ConstantValue<*>>?,
        useSiteTarget: AnnotationUseSiteTarget? = null
    ): FirAnnotation {
        return buildAnnotation {
            source = KtRealPsiSourceElement(ktAnnotation)
            annotationTypeRef = buildResolvedTypeRef {
                coneType = classId.toLookupTag().constructClassType()
            }
            this.argumentMapping = buildAnnotationArgumentMapping {
                valueArguments?.forEach { [name, constantValue] ->
                    mapping[name] = resolveValue(ktAnnotation, constantValue)
                }
            }
            useSiteTarget?.let {
                this.useSiteTarget = it
            }
        }
    }

    private fun resolveValue(
        sourceElement: PsiElement,
        value: ConstantValue<*>
    ): FirExpression {
        return when (value) {
            is EnumValue -> sourceElement.toEnumEntryReferenceExpression(value.enumClassId, value.enumEntryName)
            is KClassValue -> buildGetClassCall {
                source = KtRealPsiSourceElement(sourceElement)
                val lookupTag = (value.value as KClassValue.Value.NormalClass).classId.toLookupTag()
                val referencedType = lookupTag.constructType()
                val resolvedType = StandardClassIds.KClass.constructClassLikeType(arrayOf(referencedType), false)
                argumentList = buildUnaryArgumentList(
                    buildClassReferenceExpression {
                        classTypeRef = buildResolvedTypeRef { coneType = referencedType }
                        coneTypeOrNull = resolvedType
                    }
                )
                coneTypeOrNull = resolvedType
            }
            is ArrayValue -> {
                buildCollectionLiteral {
                    source = KtRealPsiSourceElement(sourceElement)
                    // Not quite precise, yet doesn't require annotation resolution
                    coneTypeOrNull = (inferArrayValueType(value.value) ?: StandardTypes.Any).createArrayType()

                    argumentList = buildArgumentList {
                        value.value.mapTo(arguments) { resolveValue(sourceElement, it) }
                    }
                }
            }
            is AnnotationValue -> {
                deserializeAnnotation(
                    sourceElement,
                    value.value.classId,
                    value.value.argumentsMapping
                )
            }
            is BooleanValue -> const(ConstantValueKind.Boolean, value.value, sourceElement)
            is ByteValue -> const(ConstantValueKind.Byte, value.value, sourceElement)
            is CharValue -> const(ConstantValueKind.Char, value.value, sourceElement)
            is ShortValue -> const(ConstantValueKind.Short, value.value, sourceElement)
            is LongValue -> const(ConstantValueKind.Long, value.value, sourceElement)
            is FloatValue -> const(ConstantValueKind.Float, value.value, sourceElement)
            is DoubleValue -> const(ConstantValueKind.Double, value.value, sourceElement)
            is UByteValue -> const(ConstantValueKind.UnsignedByte, value.value, sourceElement)
            is UShortValue -> const(ConstantValueKind.UnsignedShort, value.value, sourceElement)
            is UIntValue -> const(ConstantValueKind.UnsignedInt, value.value, sourceElement)
            is ULongValue -> const(ConstantValueKind.UnsignedLong, value.value, sourceElement)
            is IntValue -> const(ConstantValueKind.Int, value.value, sourceElement)
            NullValue -> const(ConstantValueKind.Null, null, sourceElement)
            is StringValue -> const(ConstantValueKind.String, value.value, sourceElement)
            else -> errorWithAttachment("Unexpected value ${value::class}") {
                withEntry("value", value.toString())
            }
        }
    }

    private fun inferArrayValueType(values: List<ConstantValue<*>>): ConeClassLikeType? {
        if (values.isNotEmpty()) {
            val firstValue = values.first()

            for ((index, value) in values.withIndex()) {
                if (index > 0 && value.javaClass != firstValue.javaClass) {
                    return null
                }
            }

            return when (firstValue) {
                is BooleanValue -> StandardTypes.Boolean
                is ByteValue -> StandardTypes.Byte
                is CharValue -> StandardTypes.Char
                is ShortValue -> StandardTypes.Short
                is IntValue -> StandardTypes.Int
                is LongValue -> StandardTypes.Long
                is UByteValue -> StandardTypes.Byte
                is UShortValue -> StandardTypes.Short
                is UIntValue -> StandardTypes.Int
                is ULongValue -> StandardTypes.Long
                is DoubleValue -> StandardTypes.Double
                is FloatValue -> StandardTypes.Float
                is AnnotationValue -> session.builtinTypes.annotationType.coneType
                is StringValue -> StandardTypes.String
                is EnumValue -> firstValue.enumClassId.constructClassLikeType(ConeTypeProjection.EMPTY_ARRAY, isMarkedNullable = false)
                is ArrayValue -> values.firstNotNullOfOrNull { inferArrayValueType((it as ArrayValue).value) }?.createArrayType()
                is KClassValue -> {
                    val kClassType = StandardTypes.Any
                    StandardClassIds.KClass.constructClassLikeType(arrayOf(kClassType), isMarkedNullable = false)
                }
                else -> null
            }
        }

        return null
    }

    private fun const(kind: ConstantValueKind, value: Any?, sourceElement: PsiElement): FirLiteralExpression =
        buildLiteralExpression(KtRealPsiSourceElement(sourceElement), kind, value, setType = true)

    private fun PsiElement.toEnumEntryReferenceExpression(classId: ClassId, entryName: Name): FirExpression =
        buildEnumEntryDeserializedAccessExpression {
            enumClassId = classId
            enumEntryName = entryName
        }
}
