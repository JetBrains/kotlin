/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.*
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName

abstract class AbstractAnnotationDeserializer(
    private val session: FirSession
) {
    protected val protocol = BuiltInSerializerProtocol

    fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<FirAnnotationCall> {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.flags)) return emptyList()
        val annotations = classProto.getExtension(protocol.classAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    fun loadFunctionAnnotations(functionProto: ProtoBuf.Function, nameResolver: NameResolver): List<FirAnnotationCall> {
        if (!Flags.HAS_ANNOTATIONS.get(functionProto.flags)) return emptyList()
        val annotations = functionProto.getExtension(protocol.functionAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    fun loadPropertyAnnotations(propertyProto: ProtoBuf.Property, nameResolver: NameResolver): List<FirAnnotationCall> {
        if (!Flags.HAS_ANNOTATIONS.get(propertyProto.flags)) return emptyList()
        val annotations = propertyProto.getExtension(protocol.propertyAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    fun loadConstructorAnnotations(constructorProto: ProtoBuf.Constructor, nameResolver: NameResolver): List<FirAnnotationCall> {
        if (!Flags.HAS_ANNOTATIONS.get(constructorProto.flags)) return emptyList()
        val annotations = constructorProto.getExtension(protocol.constructorAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    fun loadValueParameterAnnotations(valueParameterProto: ProtoBuf.ValueParameter, nameResolver: NameResolver): List<FirAnnotationCall> {
        if (!Flags.HAS_ANNOTATIONS.get(valueParameterProto.flags)) return emptyList()
        val annotations = valueParameterProto.getExtension(protocol.parameterAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    abstract fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotationCall>

    fun deserializeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): FirAnnotationCall {
        val classId = nameResolver.getClassId(proto.id)
        val lookupTag = ConeClassLikeLookupTagImpl(classId)
        val symbol = lookupTag.toSymbol(session)
        val firAnnotationClass = (symbol as? FirClassSymbol)?.fir

        var arguments = emptyList<FirExpression>()
        if (proto.argumentCount != 0 && firAnnotationClass?.classKind == ClassKind.ANNOTATION_CLASS) {
            val constructor = firAnnotationClass.declarations.firstOrNull { it is FirConstructor }
            if (constructor is FirConstructor) {
                val parameterByName = constructor.valueParameters.associateBy { it.name }

                arguments = proto.argumentList.mapNotNull {
                    val name = nameResolver.getName(it.nameId)
                    val parameter = parameterByName[name] ?: return@mapNotNull null
                    val value = resolveValue(parameter.returnTypeRef, it.value, nameResolver) ?: return@mapNotNull null
                    FirNamedArgumentExpressionImpl(
                        session, null, name, value
                    )
                }
            }
        }

        return FirAnnotationCallImpl(
            session, null, null,
            symbol?.let {
                FirResolvedTypeRefImpl(
                    session, null, it.constructType(emptyList(), isNullable = false)
                )
            } ?: FirErrorTypeRefImpl(session, null, "Symbol not found for $classId")
        ).apply {
            this.arguments += arguments
        }
    }

    private fun resolveValue(
        expectedType: FirTypeRef, value: ProtoBuf.Annotation.Argument.Value, nameResolver: NameResolver
    ): FirExpression? {
        // TODO: val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)

        val result: FirExpression = when (value.type) {
            BYTE -> const(IrConstKind.Byte, value.intValue.toByte())
            CHAR -> const(IrConstKind.Char, value.intValue.toChar())
            SHORT -> const(IrConstKind.Short, value.intValue.toShort())
            INT -> const(IrConstKind.Int, value.intValue.toInt())
            LONG -> const(IrConstKind.Long, value.intValue)
            FLOAT -> const(IrConstKind.Float, value.floatValue)
            DOUBLE -> const(IrConstKind.Double, value.doubleValue)
            BOOLEAN -> const(IrConstKind.Boolean, (value.intValue != 0L))
            STRING -> const(IrConstKind.String, nameResolver.getString(value.stringValue))
            ANNOTATION -> deserializeAnnotation(value.annotation, nameResolver)
            CLASS -> FirGetClassCallImpl(session, null).apply {
                val classId = nameResolver.getClassId(value.classId)
                val lookupTag = ConeClassLikeLookupTagImpl(classId)
                val referencedType = lookupTag.constructType(emptyArray(), isNullable = false)
                arguments += FirClassReferenceExpressionImpl(
                    this@AbstractAnnotationDeserializer.session, null,
                    FirResolvedTypeRefImpl(
                        this@AbstractAnnotationDeserializer.session, null, referencedType
                    )
                )
            }
            ENUM -> FirFunctionCallImpl(session, null).apply {
                val classId = nameResolver.getClassId(value.classId)
                val entryName = nameResolver.getName(value.enumValueId)
                val entryClassId = classId.createNestedClassId(entryName)
                val entryLookupTag = ConeClassLikeLookupTagImpl(entryClassId)
                val entrySymbol = entryLookupTag.toSymbol(this@AbstractAnnotationDeserializer.session)
                this.calleeReference = entrySymbol?.let {
                    FirResolvedCallableReferenceImpl(this@AbstractAnnotationDeserializer.session, null, entryName, it)
                } ?: FirErrorNamedReference(
                    this@AbstractAnnotationDeserializer.session, null,
                    errorReason = "Strange deserialized enum value: $classId.$entryName"
                )
            }
//            ARRAY -> {
//                val expectedIsArray = KotlinBuiltIns.isArray(expectedType) || KotlinBuiltIns.isPrimitiveArray(expectedType)
//                val arrayElements = value.arrayElementList
//
//                val actualArrayType =
//                    if (arrayElements.isNotEmpty()) {
//                        val actualElementType = resolveArrayElementType(arrayElements.first(), nameResolver)
//                        builtIns.getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(actualElementType)
//                            ?: builtIns.getArrayType(Variance.INVARIANT, actualElementType)
//                    } else {
//                        // In the case of empty array, no element has the element type, so we fall back to the expected type, if any.
//                        // This is not very accurate when annotation class has been changed without recompiling clients,
//                        // but should not in fact matter because the value is empty anyway
//                        if (expectedIsArray) expectedType else builtIns.getArrayType(Variance.INVARIANT, builtIns.anyType)
//                    }
//
//                val expectedElementType = builtIns.getArrayElementType(if (expectedIsArray) expectedType else actualArrayType)
//
//                ConstantValueFactory.createArrayValue(
//                    arrayElements.map {
//                        resolveValue(expectedElementType, it, nameResolver)
//                    },
//                    actualArrayType
//                )
//            }
//            else -> error("Unsupported annotation argument type: ${value.type} (expected $expectedType)")
            else -> return null
        }
        return result
    }

    private fun <T> const(kind: IrConstKind<T>, value: T) = FirConstExpressionImpl(session, null, kind, value)
}