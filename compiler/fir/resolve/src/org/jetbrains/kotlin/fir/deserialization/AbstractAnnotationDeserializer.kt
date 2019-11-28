/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.impl.FirErrorNamedReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirResolvedNamedReferenceImpl
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.diagnostics.FirUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.expressions.FirConstKind
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
        val firAnnotationClass = (symbol as? FirRegularClassSymbol)?.fir

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
                        null, value, false, name
                    )
                }
            }
        }

        return FirAnnotationCallImpl(
            null, null,
            symbol?.let {
                FirResolvedTypeRefImpl(
                    null, it.constructType(emptyList(), isNullable = false)
                )
            } ?: FirErrorTypeRefImpl(null, FirUnresolvedSymbolError(classId))
        ).apply {
            this.arguments += arguments
        }
    }

    private fun resolveValue(
        expectedType: FirTypeRef, value: ProtoBuf.Annotation.Argument.Value, nameResolver: NameResolver
    ): FirExpression? {
        // TODO: val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)

        val result: FirExpression = when (value.type) {
            BYTE -> const(FirConstKind.Byte, value.intValue.toByte())
            CHAR -> const(FirConstKind.Char, value.intValue.toChar())
            SHORT -> const(FirConstKind.Short, value.intValue.toShort())
            INT -> const(FirConstKind.Int, value.intValue.toInt())
            LONG -> const(FirConstKind.Long, value.intValue)
            FLOAT -> const(FirConstKind.Float, value.floatValue)
            DOUBLE -> const(FirConstKind.Double, value.doubleValue)
            BOOLEAN -> const(FirConstKind.Boolean, (value.intValue != 0L))
            STRING -> const(FirConstKind.String, nameResolver.getString(value.stringValue))
            ANNOTATION -> deserializeAnnotation(value.annotation, nameResolver)
            CLASS -> FirGetClassCallImpl(null).apply {
                val classId = nameResolver.getClassId(value.classId)
                val lookupTag = ConeClassLikeLookupTagImpl(classId)
                val referencedType = lookupTag.constructType(emptyArray(), isNullable = false)
                arguments += FirClassReferenceExpressionImpl(
                    null,
                    FirResolvedTypeRefImpl(null, referencedType)
                )
            }
            ENUM -> FirFunctionCallImpl(null).apply {
                val classId = nameResolver.getClassId(value.classId)
                val entryName = nameResolver.getName(value.enumValueId)


                val enumLookupTag = ConeClassLikeLookupTagImpl(classId)
                val enumSymbol = enumLookupTag.toSymbol(this@AbstractAnnotationDeserializer.session)
                val firClass = enumSymbol?.fir as? FirRegularClass
                val enumEntries = firClass?.collectEnumEntries() ?: emptyList()
                val enumEntrySymbol = enumEntries.find { it.callableId.callableName == entryName }
                this.calleeReference = enumEntrySymbol?.let {
                    FirResolvedNamedReferenceImpl(null, entryName, it)
                } ?: FirErrorNamedReferenceImpl(
                    null,
                    FirSimpleDiagnostic("Strange deserialized enum value: $classId.$entryName", DiagnosticKind.DeserializationError)
                )
            }
//            ARRAY -> {
//                TODO: see AnnotationDeserializer
//            }
//            else -> error("Unsupported annotation argument type: ${value.type} (expected $expectedType)")
            else -> return null
        }
        return result
    }

    private fun <T> const(kind: FirConstKind<T>, value: T) = FirConstExpressionImpl(null, kind, value)
}
