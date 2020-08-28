/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.computeTypeAttributes
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getName

class FirDeserializationContext(
    val nameResolver: NameResolver,
    val typeTable: TypeTable,
    val versionRequirementTable: VersionRequirementTable,
    val session: FirSession,
    val packageFqName: FqName,
    val relativeClassName: FqName?,
    val typeDeserializer: FirTypeDeserializer,
    val annotationDeserializer: AbstractAnnotationDeserializer,
    val constDeserializer: FirConstDeserializer,
    val containerSource: DeserializedContainerSource?,
    outerTypeParameters: List<FirTypeParameterSymbol>,
    val components: FirDeserializationComponents
) {
    val allTypeParameters: List<FirTypeParameterSymbol> =
        typeDeserializer.ownTypeParameters + outerTypeParameters

    fun childContext(
        typeParameterProtos: List<ProtoBuf.TypeParameter>,
        nameResolver: NameResolver = this.nameResolver,
        typeTable: TypeTable = this.typeTable,
        relativeClassName: FqName? = this.relativeClassName,
        containerSource: DeserializedContainerSource? = this.containerSource,
        annotationDeserializer: AbstractAnnotationDeserializer = this.annotationDeserializer,
        capturesTypeParameters: Boolean = true
    ): FirDeserializationContext = FirDeserializationContext(
        nameResolver, typeTable, versionRequirementTable, session, packageFqName, relativeClassName,
        FirTypeDeserializer(
            session, nameResolver, typeTable, typeParameterProtos, typeDeserializer
        ),
        annotationDeserializer, constDeserializer, containerSource,
        if (capturesTypeParameters) allTypeParameters else emptyList(), components
    )

    val memberDeserializer: FirMemberDeserializer = FirMemberDeserializer(this)

    companion object {
        fun createForPackage(
            fqName: FqName,
            packageProto: ProtoBuf.Package,
            nameResolver: NameResolver,
            session: FirSession,
            annotationDeserializer: AbstractAnnotationDeserializer,
            constDeserializer: FirConstDeserializer,
            containerSource: DeserializedContainerSource?
        ) = createRootContext(
            nameResolver,
            TypeTable(packageProto.typeTable),
            session,
            annotationDeserializer,
            constDeserializer,
            fqName,
            relativeClassName = null,
            typeParameterProtos = emptyList(),
            containerSource
        )

        fun createForClass(
            classId: ClassId,
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver,
            session: FirSession,
            annotationDeserializer: AbstractAnnotationDeserializer,
            constDeserializer: FirConstDeserializer,
            containerSource: DeserializedContainerSource?
        ) = createRootContext(
            nameResolver,
            TypeTable(classProto.typeTable),
            session,
            annotationDeserializer,
            constDeserializer,
            classId.packageFqName,
            classId.relativeClassName,
            classProto.typeParameterList,
            containerSource
        )

        private fun createRootContext(
            nameResolver: NameResolver,
            typeTable: TypeTable,
            session: FirSession,
            annotationDeserializer: AbstractAnnotationDeserializer,
            constDeserializer: FirConstDeserializer,
            packageFqName: FqName,
            relativeClassName: FqName?,
            typeParameterProtos: List<ProtoBuf.TypeParameter>,
            containerSource: DeserializedContainerSource?
        ): FirDeserializationContext {
            return FirDeserializationContext(
                nameResolver, typeTable,
                VersionRequirementTable.EMPTY, // TODO:
                session,
                packageFqName,
                relativeClassName,
                FirTypeDeserializer(
                    session,
                    nameResolver,
                    typeTable,
                    typeParameterProtos,
                    null
                ),
                annotationDeserializer,
                constDeserializer,
                containerSource,
                emptyList(),
                FirDeserializationComponents()
            )
        }
    }
}

// TODO: Move something here
class FirDeserializationComponents {

}

class FirMemberDeserializer(private val c: FirDeserializationContext) {
    private val contractDeserializer = FirContractDeserializer(c)

    private fun loadOldFlags(oldFlags: Int): Int {
        val lowSixBits = oldFlags and 0x3f
        val rest = (oldFlags shr 8) shl 6
        return lowSixBits + rest
    }

    fun loadTypeAlias(proto: ProtoBuf.TypeAlias): FirTypeAlias {
        val flags = proto.flags
        val name = c.nameResolver.getName(proto.name)
        val local = c.childContext(proto.typeParameterList)
        val classId = ClassId(c.packageFqName, name)
        return buildTypeAlias {
            session = c.session
            origin = FirDeclarationOrigin.Library
            this.name = name
            status = FirResolvedDeclarationStatusImpl(
                FirProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
                Modality.FINAL
            ).apply {
                isExpect = Flags.IS_EXPECT_CLASS.get(flags)
                isActual = false
            }
            symbol = FirTypeAliasSymbol(classId)
            expandedTypeRef = buildResolvedTypeRef {
                type = local.typeDeserializer.type(proto.underlyingType(c.typeTable), ConeAttributes.Empty)
            }
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
        }
    }

    fun loadProperty(
        proto: ProtoBuf.Property,
        classProto: ProtoBuf.Class? = null
    ): FirProperty {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)
        val callableName = c.nameResolver.getName(proto.name)
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = FirPropertySymbol(callableId)
        val local = c.childContext(proto.typeParameterList)

        // Per documentation on Property.getter_flags in metadata.proto, if an accessor flags field is absent, its value should be computed
        // by taking hasAnnotations/visibility/modality from property flags, and using false for the rest
        val defaultAccessorFlags = Flags.getAccessorFlags(
            Flags.HAS_ANNOTATIONS.get(flags),
            Flags.VISIBILITY.get(flags),
            Flags.MODALITY.get(flags),
            false, false, false
        )

        val returnTypeRef = proto.returnType(c.typeTable).toTypeRef(local)

        val hasGetter = Flags.HAS_GETTER.get(flags)
        val receiverAnnotations = if (hasGetter && proto.hasReceiver()) {
            c.annotationDeserializer.loadExtensionReceiverParameterAnnotations(
                c.containerSource, proto, local.nameResolver, local.typeTable, AbstractAnnotationDeserializer.CallableKind.PROPERTY_GETTER
            )
        } else {
            emptyList()
        }

        val getter = if (hasGetter) {
            val getterFlags = if (proto.hasGetterFlags()) proto.getterFlags else defaultAccessorFlags
            val visibility = FirProtoEnumFlags.visibility(Flags.VISIBILITY.get(getterFlags))
            val modality = ProtoEnumFlags.modality(Flags.MODALITY.get(getterFlags))
            if (Flags.IS_NOT_DEFAULT.get(getterFlags)) {
                buildPropertyAccessor {
                    session = c.session
                    origin = FirDeclarationOrigin.Library
                    this.returnTypeRef = returnTypeRef
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    isGetter = true
                    status = FirResolvedDeclarationStatusImpl(visibility, modality)
                    annotations +=
                        c.annotationDeserializer.loadPropertyGetterAnnotations(
                            c.containerSource, proto, local.nameResolver, local.typeTable, getterFlags
                        )
                    this.symbol = FirPropertyAccessorSymbol()
                }
            } else {
                FirDefaultPropertyGetter(null, c.session, FirDeclarationOrigin.Library, returnTypeRef, visibility)
            }
        } else {
            null
        }

        val setter = if (Flags.HAS_SETTER.get(flags)) {
            val setterFlags = if (proto.hasSetterFlags()) proto.setterFlags else defaultAccessorFlags
            val visibility = FirProtoEnumFlags.visibility(Flags.VISIBILITY.get(setterFlags))
            val modality = ProtoEnumFlags.modality(Flags.MODALITY.get(setterFlags))
            if (Flags.IS_NOT_DEFAULT.get(setterFlags)) {
                buildPropertyAccessor {
                    session = c.session
                    origin = FirDeclarationOrigin.Library
                    this.returnTypeRef = FirImplicitUnitTypeRef(source)
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    isGetter = false
                    status = FirResolvedDeclarationStatusImpl(visibility, modality)
                    annotations +=
                        c.annotationDeserializer.loadPropertySetterAnnotations(
                            c.containerSource, proto, local.nameResolver, local.typeTable, setterFlags
                        )
                    this.symbol = FirPropertyAccessorSymbol()
                    valueParameters += local.memberDeserializer.valueParameters(
                        listOf(proto.setterValueParameter),
                        proto,
                        AbstractAnnotationDeserializer.CallableKind.PROPERTY_SETTER,
                        classProto
                    )
                }
            } else {
                FirDefaultPropertySetter(null, c.session, FirDeclarationOrigin.Library, returnTypeRef, visibility)
            }
        } else {
            null
        }

        val isVar = Flags.IS_VAR.get(flags)
        return buildProperty {
            session = c.session
            origin = FirDeclarationOrigin.Library
            this.returnTypeRef = returnTypeRef
            receiverTypeRef = proto.receiverType(c.typeTable)?.toTypeRef(local).apply {
                annotations += receiverAnnotations
            }
            name = callableName
            this.isVar = isVar
            this.symbol = symbol
            isLocal = false
            status = FirResolvedDeclarationStatusImpl(
                FirProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
                ProtoEnumFlags.modality(Flags.MODALITY.get(flags))
            ).apply {
                isExpect = Flags.IS_EXPECT_PROPERTY.get(flags)
                isActual = false
                isOverride = false
                isConst = Flags.IS_CONST.get(flags)
                isLateInit = Flags.IS_LATEINIT.get(flags)
            }

            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            annotations +=
                c.annotationDeserializer.loadPropertyAnnotations(c.containerSource, proto, local.nameResolver, local.typeTable)
            annotations +=
                c.annotationDeserializer.loadPropertyBackingFieldAnnotations(
                    c.containerSource, proto, local.nameResolver, local.typeTable
                )
            annotations +=
                c.annotationDeserializer.loadPropertyDelegatedFieldAnnotations(
                    c.containerSource, proto, local.nameResolver, local.typeTable
                )
            this.getter = getter
            this.setter = setter
            this.containerSource = c.containerSource
            this.initializer = c.constDeserializer.loadConstant(proto, symbol.callableId, c.nameResolver)
        }
    }

    fun loadFunction(
        proto: ProtoBuf.Function,
        classProto: ProtoBuf.Class? = null
    ): FirSimpleFunction {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)

        val receiverAnnotations = if (proto.hasReceiver()) {
            c.annotationDeserializer.loadExtensionReceiverParameterAnnotations(
                c.containerSource, proto, c.nameResolver, c.typeTable, AbstractAnnotationDeserializer.CallableKind.OTHERS
            )
        } else {
            emptyList()
        }

        val versionRequirementTable =
            // TODO: Support case for KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME
            c.versionRequirementTable

        val callableName = c.nameResolver.getName(proto.name)
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = FirNamedFunctionSymbol(callableId)
        val local = c.childContext(proto.typeParameterList)
        // TODO: support contracts

        val simpleFunction = buildSimpleFunction {
            session = c.session
            origin = FirDeclarationOrigin.Library
            returnTypeRef = proto.returnType(local.typeTable).toTypeRef(local)
            receiverTypeRef = proto.receiverType(local.typeTable)?.toTypeRef(local).apply {
                annotations += receiverAnnotations
            }
            name = callableName
            status = FirResolvedDeclarationStatusImpl(
                FirProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
                ProtoEnumFlags.modality(Flags.MODALITY.get(flags))
            ).apply {
                isExpect = Flags.IS_EXPECT_FUNCTION.get(flags)
                isActual = false
                isOverride = false
                isOperator = Flags.IS_OPERATOR.get(flags)
                isInfix = Flags.IS_INFIX.get(flags)
                isInline = Flags.IS_INLINE.get(flags)
                isTailRec = Flags.IS_TAILREC.get(flags)
                isExternal = Flags.IS_EXTERNAL_FUNCTION.get(flags)
                isSuspend = Flags.IS_SUSPEND.get(flags)
            }
            this.symbol = symbol
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            valueParameters += local.memberDeserializer.valueParameters(
                proto.valueParameterList,
                proto,
                AbstractAnnotationDeserializer.CallableKind.OTHERS,
                classProto
            )
            annotations +=
                c.annotationDeserializer.loadFunctionAnnotations(c.containerSource, proto, local.nameResolver, local.typeTable)
            this.containerSource = c.containerSource
        }
        if (proto.hasContract()) {
            val contractDescription = contractDeserializer.loadContract(proto.contract, simpleFunction)
            if (contractDescription != null) {
                simpleFunction.replaceContractDescription(contractDescription)
            }
        }
        return simpleFunction
    }

    fun loadConstructor(
        proto: ProtoBuf.Constructor,
        classProto: ProtoBuf.Class,
        classBuilder: FirRegularClassBuilder
    ): FirConstructor {
        val flags = proto.flags
        val relativeClassName = c.relativeClassName!!
        val callableId = CallableId(c.packageFqName, relativeClassName, relativeClassName.shortName())
        val symbol = FirConstructorSymbol(callableId)
        val local = c.childContext(emptyList())
        val isPrimary = !Flags.IS_SECONDARY.get(flags)

        val typeParameters = classBuilder.typeParameters

        val delegatedSelfType = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                classBuilder.symbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
        }

        return if (isPrimary) {
            FirPrimaryConstructorBuilder()
        } else {
            FirConstructorBuilder()
        }.apply {
            session = c.session
            origin = FirDeclarationOrigin.Library
            returnTypeRef = delegatedSelfType
            val visibility = FirProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags))
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL
            ).apply {
                isExpect = Flags.IS_EXPECT_FUNCTION.get(flags)
                isActual = false
                isOverride = false
                isInner = classBuilder.status.isInner
            }
            this.symbol = symbol
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            this.typeParameters +=
                typeParameters.filterIsInstance<FirTypeParameter>()
                    .map { buildConstructedClassTypeParameterRef { this.symbol = it.symbol } }
            valueParameters += local.memberDeserializer.valueParameters(
                proto.valueParameterList,
                proto,
                AbstractAnnotationDeserializer.CallableKind.OTHERS,
                classProto,
                addDefaultValue = classBuilder.symbol.classId == StandardClassIds.Enum
            )
            annotations +=
                c.annotationDeserializer.loadConstructorAnnotations(c.containerSource, proto, local.nameResolver, local.typeTable)
        }.build()
    }

    private fun defaultValue(flags: Int): FirExpression? {
        if (Flags.DECLARES_DEFAULT_VALUE.get(flags)) {
            return buildExpressionStub()
        }
        return null
    }

    private fun valueParameters(
        valueParameters: List<ProtoBuf.ValueParameter>,
        callableProto: MessageLite,
        callableKind: AbstractAnnotationDeserializer.CallableKind,
        classProto: ProtoBuf.Class?,
        addDefaultValue: Boolean = false
    ): List<FirValueParameter> {
        return valueParameters.mapIndexed { index, proto ->
            val flags = if (proto.hasFlags()) proto.flags else 0
            val name = c.nameResolver.getName(proto.name)
            buildValueParameter {
                session = c.session
                origin = FirDeclarationOrigin.Library
                returnTypeRef = proto.type(c.typeTable).toTypeRef(c)
                this.name = name
                symbol = FirVariableSymbol(name)
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                defaultValue = defaultValue(flags)
                if (addDefaultValue) {
                    defaultValue = buildExpressionStub()
                }
                isCrossinline = Flags.IS_CROSSINLINE.get(flags)
                isNoinline = Flags.IS_NOINLINE.get(flags)
                isVararg = proto.varargElementType(c.typeTable) != null
                annotations += c.annotationDeserializer.loadValueParameterAnnotations(
                    c.containerSource,
                    callableProto,
                    proto,
                    classProto,
                    c.nameResolver,
                    c.typeTable,
                    callableKind,
                    index,
                )
            }
        }.toList()
    }

    private fun ProtoBuf.Type.toTypeRef(context: FirDeserializationContext): FirTypeRef {
        return buildResolvedTypeRef {
            annotations += context.annotationDeserializer.loadTypeAnnotations(this@toTypeRef, context.nameResolver)
            val attributes = annotations.computeTypeAttributes()
            type = context.typeDeserializer.type(this@toTypeRef, attributes)
        }
    }

}

