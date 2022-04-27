/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.*
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.ConstantValueKind

abstract class AbstractAnnotationDeserializer(
    private val session: FirSession
) {
    protected val protocol = BuiltInSerializerProtocol

    open fun inheritAnnotationInfo(parent: AbstractAnnotationDeserializer) {
    }

    enum class CallableKind {
        PROPERTY,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        OTHERS
    }

    fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.flags)) return emptyList()
        val annotations = classProto.getExtension(protocol.classAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    fun loadTypeAliasAnnotations(aliasProto: ProtoBuf.TypeAlias, nameResolver: NameResolver): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(aliasProto.flags)) return emptyList()
        return aliasProto.annotationList.map { deserializeAnnotation(it, nameResolver) }
    }

    open fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(functionProto.flags)) return emptyList()
        val annotations = functionProto.getExtension(protocol.functionAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    open fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        containingClassProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(propertyProto.flags)) return emptyList()
        val annotations = propertyProto.getExtension(protocol.propertyAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver, AnnotationUseSiteTarget.PROPERTY) }
    }

    open fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        return emptyList()
    }

    open fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        return emptyList()
    }

    open fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int
    ): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(getterFlags)) return emptyList()
        val annotations = propertyProto.getExtension(protocol.propertyGetterAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver, AnnotationUseSiteTarget.PROPERTY_GETTER) }
    }

    open fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int
    ): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(setterFlags)) return emptyList()
        val annotations = propertyProto.getExtension(protocol.propertySetterAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver, AnnotationUseSiteTarget.PROPERTY_SETTER) }
    }

    open fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(constructorProto.flags)) return emptyList()
        val annotations = constructorProto.getExtension(protocol.constructorAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    open fun loadValueParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        valueParameterProto: ProtoBuf.ValueParameter,
        classProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
        parameterIndex: Int
    ): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(valueParameterProto.flags)) return emptyList()
        val annotations = valueParameterProto.getExtension(protocol.parameterAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    open fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind
    ): List<FirAnnotation> {
        return emptyList()
    }

    abstract fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotation>

    fun deserializeAnnotation(
        proto: ProtoBuf.Annotation,
        nameResolver: NameResolver,
        useSiteTarget: AnnotationUseSiteTarget? = null
    ): FirAnnotation {
        val classId = nameResolver.getClassId(proto.id)
        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                type = ConeClassLikeLookupTagImpl(classId).constructClassType(emptyArray(), isNullable = false)
            }
            this.argumentMapping = createArgumentMapping(proto, classId, nameResolver)
            useSiteTarget?.let {
                this.useSiteTarget = it
            }
        }
    }

    private fun createArgumentMapping(
        proto: ProtoBuf.Annotation,
        classId: ClassId,
        nameResolver: NameResolver
    ): FirAnnotationArgumentMapping {
        return buildAnnotationArgumentMapping build@{
            if (proto.argumentCount == 0) return@build
            // Used only for annotation parameters of array types
            // Avoid triggering it in other cases, since it's quite expensive
            val parameterByName: Map<Name, FirValueParameter>? by lazy(LazyThreadSafetyMode.NONE) {
                val lookupTag = ConeClassLikeLookupTagImpl(classId)
                val symbol = lookupTag.toSymbol(session)
                val firAnnotationClass = (symbol as? FirRegularClassSymbol)?.fir ?: return@lazy null

                val classScope =
                    firAnnotationClass.defaultType().scope(session, ScopeSession(), FakeOverrideTypeCalculator.DoNothing)
                        ?: error("Null scope for $classId")

                val constructor =
                    classScope.getDeclaredConstructors().singleOrNull()?.fir ?: error("No single constructor found for $classId")

                constructor.valueParameters.associateBy { it.name }
            }

            proto.argumentList.mapNotNull {
                val name = nameResolver.getName(it.nameId)
                val value = resolveValue(it.value, nameResolver) { parameterByName?.get(name)?.returnTypeRef?.coneType }
                name to value
            }.toMap(mapping)
        }
    }

    private fun resolveValue(
        value: ProtoBuf.Annotation.Argument.Value, nameResolver: NameResolver, expectedType: () -> ConeKotlinType?
    ): FirExpression {
        val isUnsigned = Flags.IS_UNSIGNED.get(value.flags)

        return when (value.type) {
            BYTE -> {
                val kind = if (isUnsigned) ConstantValueKind.UnsignedByte else ConstantValueKind.Byte
                const(kind, value.intValue.toByte(), session.builtinTypes.byteType)
            }

            SHORT -> {
                val kind = if (isUnsigned) ConstantValueKind.UnsignedShort else ConstantValueKind.Short
                const(kind, value.intValue.toShort(), session.builtinTypes.shortType)
            }

            INT -> {
                val kind = if (isUnsigned) ConstantValueKind.UnsignedInt else ConstantValueKind.Int
                const(kind, value.intValue.toInt(), session.builtinTypes.intType)
            }

            LONG -> {
                val kind = if (isUnsigned) ConstantValueKind.UnsignedLong else ConstantValueKind.Long
                const(kind, value.intValue, session.builtinTypes.longType)
            }

            CHAR -> const(ConstantValueKind.Char, value.intValue.toInt().toChar(), session.builtinTypes.charType)
            FLOAT -> const(ConstantValueKind.Float, value.floatValue, session.builtinTypes.floatType)
            DOUBLE -> const(ConstantValueKind.Double, value.doubleValue, session.builtinTypes.doubleType)
            BOOLEAN -> const(ConstantValueKind.Boolean, (value.intValue != 0L), session.builtinTypes.booleanType)
            STRING -> const(ConstantValueKind.String, nameResolver.getString(value.stringValue), session.builtinTypes.stringType)
            ANNOTATION -> deserializeAnnotation(value.annotation, nameResolver)
            CLASS -> buildGetClassCall {
                val classId = nameResolver.getClassId(value.classId)
                val lookupTag = ConeClassLikeLookupTagImpl(classId)
                val referencedType = lookupTag.constructType(emptyArray(), isNullable = false)
                val resolvedTypeRef = buildResolvedTypeRef {
                    type = StandardClassIds.KClass.constructClassLikeType(arrayOf(referencedType), false)
                }
                argumentList = buildUnaryArgumentList(
                    buildClassReferenceExpression {
                        classTypeRef = buildResolvedTypeRef { type = referencedType }
                        typeRef = resolvedTypeRef
                    }
                )
                typeRef = resolvedTypeRef
            }
            ENUM -> buildFunctionCall {
                val classId = nameResolver.getClassId(value.classId)
                val entryName = nameResolver.getName(value.enumValueId)

                val enumLookupTag = ConeClassLikeLookupTagImpl(classId)
                val enumSymbol = enumLookupTag.toSymbol(session)
                val firClass = enumSymbol?.fir as? FirRegularClass
                val enumEntries = firClass?.collectEnumEntries() ?: emptyList()
                val enumEntrySymbol = enumEntries.find { it.name == entryName }
                calleeReference = enumEntrySymbol?.let {
                    buildResolvedNamedReference {
                        name = entryName
                        resolvedSymbol = it.symbol
                    }
                } ?: buildErrorNamedReference {
                    diagnostic =
                        ConeSimpleDiagnostic("Strange deserialized enum value: $classId.$entryName", DiagnosticKind.DeserializationError)
                }
                if (enumEntrySymbol != null) {
                    typeRef = enumEntrySymbol.returnTypeRef
                }
            }
            ARRAY -> {
                val expectedArrayElementType = expectedType()?.arrayElementType() ?: session.builtinTypes.anyType.type
                buildArrayOfCall {
                    argumentList = buildArgumentList {
                        value.arrayElementList.mapTo(arguments) { resolveValue(it, nameResolver) { expectedArrayElementType } }
                    }
                    typeRef = buildResolvedTypeRef {
                        type = expectedArrayElementType.createArrayType()
                    }
                }
            }

            else -> error("Unsupported annotation argument type: ${value.type} (expected $expectedType)")
        }
    }

    private fun <T> const(kind: ConstantValueKind<T>, value: T, typeRef: FirResolvedTypeRef): FirConstExpression<T> {
        return buildConstExpression(null, kind, value, setType = true).apply { this.replaceTypeRef(typeRef) }
    }
}
