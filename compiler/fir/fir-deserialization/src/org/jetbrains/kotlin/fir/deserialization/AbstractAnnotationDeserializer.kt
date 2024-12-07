/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite.ExtendableMessage
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.ConstantValueKind

abstract class AbstractAnnotationDeserializer(
    private val session: FirSession,
    protected val protocol: SerializerExtensionProtocol
) {
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
        return propertyProto.loadAnnotations(
            protocol.propertyAnnotation, propertyProto.flags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY
        )
    }

    open fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotations(
            protocol.propertyBackingFieldAnnotation, propertyProto.flags, nameResolver,
            AnnotationUseSiteTarget.FIELD
        )
    }

    open fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotations(
            protocol.propertyDelegatedFieldAnnotation, propertyProto.flags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
        )
    }

    open fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int,
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotations(
            protocol.propertyGetterAnnotation, getterFlags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY_GETTER
        )
    }

    open fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotations(
            protocol.propertySetterAnnotation, setterFlags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY_SETTER
        )
    }

    open fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        return constructorProto.loadAnnotations(protocol.constructorAnnotation, constructorProto.flags, nameResolver)
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
        return valueParameterProto.loadAnnotations(protocol.parameterAnnotation, valueParameterProto.flags, nameResolver)
    }

    open fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
    ): List<FirAnnotation> {
        return when (callableProto) {
            is ProtoBuf.Property -> callableProto.loadAnnotations(
                protocol.propertyExtensionReceiverAnnotation, callableProto.flags, nameResolver,
            )
            is ProtoBuf.Function -> callableProto.loadAnnotations(
                protocol.functionExtensionReceiverAnnotation, callableProto.flags, nameResolver,
            )
            else -> emptyList()
        }
    }

    open fun loadAnnotationPropertyDefaultValue(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        expectedPropertyType: FirTypeRef,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): FirExpression? {
        return null
    }

    abstract fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotation>

    open fun loadTypeParameterAnnotations(typeParameterProto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<FirAnnotation> =
        emptyList<FirAnnotation>()

    private fun <T : ExtendableMessage<T>> T.loadAnnotations(
        extension: GeneratedMessageLite.GeneratedExtension<T, List<ProtoBuf.Annotation>>?,
        flags: Int,
        nameResolver: NameResolver,
        useSiteTarget: AnnotationUseSiteTarget? = null
    ): List<FirAnnotation> {
        if (extension == null || !Flags.HAS_ANNOTATIONS.get(flags)) return emptyList()
        val annotations = getExtension(extension)
        return annotations.map { deserializeAnnotation(it, nameResolver, useSiteTarget) }
    }

    fun deserializeAnnotation(
        proto: ProtoBuf.Annotation,
        nameResolver: NameResolver,
        useSiteTarget: AnnotationUseSiteTarget? = null
    ): FirAnnotation {
        val classId = nameResolver.getClassId(proto.id)
        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                coneType = classId.toLookupTag().constructClassType()
            }
            session.lazyDeclarationResolver.disableLazyResolveContractChecksInside {
                this.argumentMapping = createArgumentMapping(proto, classId, nameResolver)
            }
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
            ARRAY -> {
                val expectedArrayElementType = expectedType()?.arrayElementType() ?: session.builtinTypes.anyType.coneType
                buildArrayLiteral {
                    argumentList = buildArgumentList {
                        value.arrayElementList.mapTo(arguments) { resolveValue(it, nameResolver) { expectedArrayElementType } }
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
