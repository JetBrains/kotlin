/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildUnaryArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildFromMissingDependenciesNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationEntryStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassTypeBean
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

class StubBasedAnnotationDeserializer(
    private val session: FirSession,
) {
    fun loadAnnotations(
        ktAnnotated: KtAnnotated,
    ): List<FirAnnotation> {
        val annotations = ktAnnotated.annotationEntries
        if (annotations.isEmpty()) return emptyList()
        return annotations.map { deserializeAnnotation(it) }
    }

    private val constantCache = mutableMapOf<CallableId, FirExpression>()

    fun loadConstant(property: KtProperty, callableId: CallableId): FirExpression? {
        if (!property.hasModifier(KtTokens.CONST_KEYWORD)) return null
        constantCache[callableId]?.let { return it }
        val propertyStub = (property.stub ?: loadStubByElement(property)) as? KotlinPropertyStubImpl ?: return null
        val constantValue = propertyStub.constantInitializer ?: return null
        return resolveValue(property, constantValue)
    }

    private fun deserializeAnnotation(
        ktAnnotation: KtAnnotationEntry
    ): FirAnnotation {
        return deserializeAnnotation(
            ktAnnotation,
            getAnnotationClassId(ktAnnotation),
            ((ktAnnotation.stub ?: loadStubByElement(ktAnnotation)) as? KotlinAnnotationEntryStubImpl)?.valueArguments,
            ktAnnotation.useSiteTarget?.getAnnotationUseSiteTarget()
        )
    }

    fun getAnnotationClassId(ktAnnotation: KtAnnotationEntry): ClassId {
        val userType = ktAnnotation.getStubOrPsiChild(KtStubElementTypes.CONSTRUCTOR_CALLEE)
            ?.getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE)
            ?.getStubOrPsiChild(KtStubElementTypes.USER_TYPE)!!
        return userType.classId()
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
                type = classId.toLookupTag().constructClassType(ConeTypeProjection.EMPTY_ARRAY, isNullable = false)
            }
            this.argumentMapping = buildAnnotationArgumentMapping {
                valueArguments?.forEach { (name, constantValue) ->
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
                val referencedType = lookupTag.constructType(ConeTypeProjection.EMPTY_ARRAY, isNullable = false)
                val resolvedType = StandardClassIds.KClass.constructClassLikeType(arrayOf(referencedType), false)
                argumentList = buildUnaryArgumentList(
                    buildClassReferenceExpression {
                        classTypeRef = buildResolvedTypeRef { type = referencedType }
                        coneTypeOrNull = resolvedType
                    }
                )
                coneTypeOrNull = resolvedType
            }
            is ArrayValue -> {
                buildArrayLiteral {
                    source = KtRealPsiSourceElement(sourceElement)
                    // Not quite precise, yet doesn't require annotation resolution
                    coneTypeOrNull = (inferArrayValueType(value.value) ?: session.builtinTypes.anyType.type).createArrayType()

                    argumentList = buildArgumentList {
                        value.value.mapTo(arguments) { resolveValue(sourceElement, it) }
                    }
                }
            }
            is AnnotationValue -> {
                deserializeAnnotation(
                    sourceElement,
                    (value.value.type as KotlinClassTypeBean).classId,
                    value.value.argumentsMapping
                )
            }
            is BooleanValue -> const(ConstantValueKind.Boolean, value.value, session.builtinTypes.booleanType, sourceElement)
            is ByteValue -> const(ConstantValueKind.Byte, value.value, session.builtinTypes.byteType, sourceElement)
            is CharValue -> const(ConstantValueKind.Char, value.value, session.builtinTypes.charType, sourceElement)
            is ShortValue -> const(ConstantValueKind.Short, value.value, session.builtinTypes.shortType, sourceElement)
            is LongValue -> const(ConstantValueKind.Long, value.value, session.builtinTypes.longType, sourceElement)
            is FloatValue -> const(ConstantValueKind.Float, value.value, session.builtinTypes.floatType, sourceElement)
            is DoubleValue -> const(ConstantValueKind.Double, value.value, session.builtinTypes.doubleType, sourceElement)
            is UByteValue -> const(ConstantValueKind.UnsignedByte, value.value, session.builtinTypes.byteType, sourceElement)
            is UShortValue -> const(ConstantValueKind.UnsignedShort, value.value, session.builtinTypes.shortType, sourceElement)
            is UIntValue -> const(ConstantValueKind.UnsignedInt, value.value, session.builtinTypes.intType, sourceElement)
            is ULongValue -> const(ConstantValueKind.UnsignedLong, value.value, session.builtinTypes.longType, sourceElement)
            is IntValue -> const(ConstantValueKind.Int, value.value, session.builtinTypes.intType, sourceElement)
            NullValue -> const(ConstantValueKind.Null, null, session.builtinTypes.nullableAnyType, sourceElement)
            is StringValue -> const(ConstantValueKind.String, value.value, session.builtinTypes.stringType, sourceElement)
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
                is BooleanValue -> session.builtinTypes.booleanType.type
                is ByteValue -> session.builtinTypes.byteType.type
                is CharValue -> session.builtinTypes.charType.type
                is ShortValue -> session.builtinTypes.shortType.type
                is IntValue -> session.builtinTypes.intType.type
                is LongValue -> session.builtinTypes.longType.type
                is UByteValue -> session.builtinTypes.byteType.type
                is UShortValue -> session.builtinTypes.shortType.type
                is UIntValue -> session.builtinTypes.intType.type
                is ULongValue -> session.builtinTypes.longType.type
                is DoubleValue -> session.builtinTypes.doubleType.type
                is FloatValue -> session.builtinTypes.floatType.type
                is AnnotationValue -> session.builtinTypes.annotationType.type
                is StringValue -> session.builtinTypes.stringType.type
                is EnumValue -> firstValue.enumClassId.constructClassLikeType(ConeTypeProjection.EMPTY_ARRAY, isNullable = false)
                is ArrayValue -> values.firstNotNullOfOrNull { inferArrayValueType((it as ArrayValue).value) }?.createArrayType()
                is KClassValue -> {
                    val kClassType = session.builtinTypes.anyType.type
                    StandardClassIds.KClass.constructClassLikeType(arrayOf(kClassType), isNullable = false)
                }
                else -> null
            }
        }

        return null
    }

    private fun <T> const(
        kind: ConstantValueKind<T>,
        value: T,
        typeRef: FirResolvedTypeRef,
        sourceElement: PsiElement
    ): FirConstExpression<T> {
        return buildConstExpression(
            KtRealPsiSourceElement(sourceElement),
            kind,
            value,
            setType = true
        ).apply { this.replaceConeTypeOrNull(typeRef.type) }
    }

    private fun PsiElement.toEnumEntryReferenceExpression(classId: ClassId, entryName: Name): FirExpression {
        return buildPropertyAccessExpression {
            source = KtRealPsiSourceElement(this@toEnumEntryReferenceExpression)
            val enumLookupTag = classId.toLookupTag()
            val enumSymbol = enumLookupTag.toSymbol(session)
            val firClass = enumSymbol?.fir as? FirRegularClass
            val enumEntries = firClass?.collectEnumEntries() ?: emptyList()
            val enumEntrySymbol = enumEntries.find { it.name == entryName }
            calleeReference = enumEntrySymbol?.let {
                buildResolvedNamedReference {
                    name = entryName
                    resolvedSymbol = it.symbol
                }
            } ?: buildFromMissingDependenciesNamedReference {
                name = entryName
            }
            if (enumEntrySymbol != null) {
                coneTypeOrNull = enumEntrySymbol.returnTypeRef.coneTypeOrNull
            }
        }
    }
}
