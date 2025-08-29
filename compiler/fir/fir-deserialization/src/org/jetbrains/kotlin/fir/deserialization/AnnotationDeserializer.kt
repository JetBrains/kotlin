/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
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
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.ConstantValueKind

interface AnnotationDeserializer {
    fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<FirAnnotation>

    fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation>

    fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        containingClassProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation>

    fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation>

    fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation>

    fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int,
    ): List<FirAnnotation>

    fun loadValueParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        valueParameterProto: ProtoBuf.ValueParameter,
        classProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
        parameterIndex: Int,
    ): List<FirAnnotation>

    fun loadEnumEntryAnnotations(
        classId: ClassId,
        enumEntryProto: ProtoBuf.EnumEntry,
        nameResolver: NameResolver,
    ): List<FirAnnotation>

    fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
    ): List<FirAnnotation>

    fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int,
    ): List<FirAnnotation>

    fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation>

    fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotation> =
        emptyList<FirAnnotation>()

    fun loadTypeParameterAnnotations(typeParameterProto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<FirAnnotation> =
        emptyList<FirAnnotation>()

    fun inheritAnnotationInfo(parent: AnnotationDeserializer) {}

    enum class CallableKind {
        PROPERTY,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        OTHERS
    }

    fun loadTypeAliasAnnotations(aliasProto: ProtoBuf.TypeAlias, nameResolver: NameResolver, session: FirSession): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(aliasProto.flags)) return emptyList()
        return aliasProto.annotationList.map { deserializeAnnotation(it, nameResolver, session) }
    }

    fun loadAnnotationPropertyDefaultValue(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        expectedPropertyType: FirTypeRef,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): FirExpression? =
        null


    fun deserializeAnnotation(
        proto: ProtoBuf.Annotation,
        nameResolver: NameResolver,
        session: FirSession,
        useSiteTarget: AnnotationUseSiteTarget? = null,
    ): FirAnnotation {
        val classId = nameResolver.getClassId(proto.id)
        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                coneType = classId.toLookupTag().constructClassType()
            }
            session.lazyDeclarationResolver.disableLazyResolveContractChecksInside {
                this.argumentMapping = createArgumentMapping(proto, classId, nameResolver, session)
            }
            useSiteTarget?.let {
                this.useSiteTarget = it
            }
        }
    }

    private fun createArgumentMapping(
        proto: ProtoBuf.Annotation,
        classId: ClassId,
        nameResolver: NameResolver,
        session: FirSession,
    ): FirAnnotationArgumentMapping {
        return buildAnnotationArgumentMapping build@{
            if (proto.argumentCount == 0) return@build
            // Used only for annotation parameters of array types
            // Avoid triggering it in other cases, since it's quite expensive
            val parameterByName: Map<Name, FirValueParameter>? by lazy(LazyThreadSafetyMode.NONE) {
                val lookupTag = classId.toLookupTag()
                val symbol = lookupTag.toSymbol(session)
                val firAnnotationClass = (symbol as? FirRegularClassSymbol)?.fir ?: return@lazy null

                val classScope = firAnnotationClass.defaultType().scope(
                    useSiteSession = session,
                    scopeSession = ScopeSession(),
                    callableCopyTypeCalculator = CallableCopyTypeCalculator.DoNothing,
                    requiredMembersPhase = null,
                ) ?: error("Null scope for $classId")

                val constructor = classScope.getDeclaredConstructors()
                    .singleOrNull()
                    ?.fir
                    ?: error("No single constructor found for $classId")

                constructor.valueParameters.associateBy { it.name }
            }

            proto.argumentList.mapNotNull {
                val name = nameResolver.getName(it.nameId)
                val value = resolveValue(it.value, nameResolver, session) { parameterByName?.get(name)?.returnTypeRef?.coneType }
                name to value
            }.toMap(mapping)
        }
    }

    private fun resolveValue(
        value: ProtoBuf.Annotation.Argument.Value,
        nameResolver: NameResolver,
        session: FirSession,
        expectedType: () -> ConeKotlinType?,
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
            ANNOTATION -> deserializeAnnotation(value.annotation, nameResolver, session)
            CLASS -> buildGetClassCall {
                val classId = nameResolver.getClassId(value.classId)
                val lookupTag = classId.toLookupTag()
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
            ENUM -> buildEnumEntryDeserializedAccessExpression {
                enumClassId = nameResolver.getClassId(value.classId)
                enumEntryName = nameResolver.getName(value.enumValueId)
            }
            ProtoBuf.Annotation.Argument.Value.Type.ARRAY -> {
                val expectedArrayElementType = expectedType()?.arrayElementType() ?: session.builtinTypes.anyType.coneType
                buildArrayLiteral {
                    argumentList = buildArgumentList {
                        value.arrayElementList.mapTo(arguments) { resolveValue(it, nameResolver, session) { expectedArrayElementType } }
                    }
                    coneTypeOrNull = expectedArrayElementType.createArrayType()
                }
            }

            else -> error("Unsupported annotation argument type: ${value.type} (expected $expectedType)")
        }
    }

    private fun const(kind: ConstantValueKind, value: Any?, typeRef: FirResolvedTypeRef): FirLiteralExpression {
        return buildLiteralExpression(null, kind, value, setType = true).apply { this.replaceConeTypeOrNull(typeRef.coneType) }
    }
}
