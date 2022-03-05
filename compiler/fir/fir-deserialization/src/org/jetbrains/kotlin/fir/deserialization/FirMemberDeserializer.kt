/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.computeTypeAttributes
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getName

class FirDeserializationContext(
    val nameResolver: NameResolver,
    val typeTable: TypeTable,
    val versionRequirementTable: VersionRequirementTable,
    val moduleData: FirModuleData,
    val packageFqName: FqName,
    val relativeClassName: FqName?,
    val typeDeserializer: FirTypeDeserializer,
    val annotationDeserializer: AbstractAnnotationDeserializer,
    val constDeserializer: FirConstDeserializer,
    val containerSource: DeserializedContainerSource?,
    val outerClassSymbol: FirRegularClassSymbol?,
    val outerTypeParameters: List<FirTypeParameterSymbol>
) {
    val session: FirSession = moduleData.session

    val allTypeParameters: List<FirTypeParameterSymbol> =
        typeDeserializer.ownTypeParameters + outerTypeParameters

    fun childContext(
        typeParameterProtos: List<ProtoBuf.TypeParameter>,
        nameResolver: NameResolver = this.nameResolver,
        typeTable: TypeTable = this.typeTable,
        relativeClassName: FqName? = this.relativeClassName,
        containerSource: DeserializedContainerSource? = this.containerSource,
        outerClassSymbol: FirRegularClassSymbol? = this.outerClassSymbol,
        annotationDeserializer: AbstractAnnotationDeserializer = this.annotationDeserializer,
        capturesTypeParameters: Boolean = true,
        containingDeclarationSymbol: FirBasedSymbol<*>? = this.outerClassSymbol
    ): FirDeserializationContext = FirDeserializationContext(
        nameResolver,
        typeTable,
        versionRequirementTable,
        moduleData,
        packageFqName,
        relativeClassName,
        FirTypeDeserializer(
            moduleData,
            nameResolver,
            typeTable,
            annotationDeserializer,
            typeParameterProtos,
            typeDeserializer,
            containingDeclarationSymbol
        ),
        annotationDeserializer,
        constDeserializer,
        containerSource,
        outerClassSymbol,
        if (capturesTypeParameters) allTypeParameters else emptyList()
    )

    val memberDeserializer: FirMemberDeserializer = FirMemberDeserializer(this)
    val dispatchReceiver = relativeClassName?.let { ClassId(packageFqName, it, false).defaultType(allTypeParameters) }

    companion object {
        fun createForPackage(
            fqName: FqName,
            packageProto: ProtoBuf.Package,
            nameResolver: NameResolver,
            moduleData: FirModuleData,
            annotationDeserializer: AbstractAnnotationDeserializer,
            constDeserializer: FirConstDeserializer,
            containerSource: DeserializedContainerSource?
        ): FirDeserializationContext = createRootContext(
            nameResolver,
            TypeTable(packageProto.typeTable),
            moduleData,
            VersionRequirementTable.create(packageProto.versionRequirementTable),
            annotationDeserializer,
            constDeserializer,
            fqName,
            relativeClassName = null,
            typeParameterProtos = emptyList(),
            containerSource,
            outerClassSymbol = null,
            containingDeclarationSymbol = null
        )

        fun createForClass(
            classId: ClassId,
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver,
            moduleData: FirModuleData,
            annotationDeserializer: AbstractAnnotationDeserializer,
            constDeserializer: FirConstDeserializer,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: FirRegularClassSymbol
        ): FirDeserializationContext = createRootContext(
            nameResolver,
            TypeTable(classProto.typeTable),
            moduleData,
            VersionRequirementTable.create(classProto.versionRequirementTable),
            annotationDeserializer,
            constDeserializer,
            classId.packageFqName,
            classId.relativeClassName,
            classProto.typeParameterList,
            containerSource,
            outerClassSymbol,
            outerClassSymbol
        )

        private fun createRootContext(
            nameResolver: NameResolver,
            typeTable: TypeTable,
            moduleData: FirModuleData,
            versionRequirementTable: VersionRequirementTable,
            annotationDeserializer: AbstractAnnotationDeserializer,
            constDeserializer: FirConstDeserializer,
            packageFqName: FqName,
            relativeClassName: FqName?,
            typeParameterProtos: List<ProtoBuf.TypeParameter>,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: FirRegularClassSymbol?,
            containingDeclarationSymbol: FirBasedSymbol<*>?
        ): FirDeserializationContext {
            return FirDeserializationContext(
                nameResolver, typeTable,
                versionRequirementTable,
                moduleData,
                packageFqName,
                relativeClassName,
                FirTypeDeserializer(
                    moduleData,
                    nameResolver,
                    typeTable,
                    annotationDeserializer,
                    typeParameterProtos,
                    null,
                    containingDeclarationSymbol
                ),
                annotationDeserializer,
                constDeserializer,
                containerSource,
                outerClassSymbol,
                emptyList()
            )
        }
    }
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
        val classId = ClassId(c.packageFqName, name)
        val symbol = FirTypeAliasSymbol(classId)
        val local = c.childContext(proto.typeParameterList, containingDeclarationSymbol = symbol)
        return buildTypeAlias {
            moduleData = c.moduleData
            origin = FirDeclarationOrigin.Library
            this.name = name
            val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags))
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                visibility.toEffectiveVisibility(owner = null)
            ).apply {
                isExpect = Flags.IS_EXPECT_CLASS.get(flags)
                isActual = false
            }

            annotations += c.annotationDeserializer.loadTypeAliasAnnotations(proto, local.nameResolver)
            this.symbol = symbol
            expandedTypeRef = proto.underlyingType(c.typeTable).toTypeRef(local)
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
        }.apply {
            versionRequirementsTable = c.versionRequirementTable
            sourceElement = c.containerSource
        }
    }

    private fun loadPropertyGetter(
        proto: ProtoBuf.Property,
        classSymbol: FirClassSymbol<*>?,
        defaultAccessorFlags: Int,
        returnTypeRef: FirTypeRef,
        propertySymbol: FirPropertySymbol,
        local: FirDeserializationContext,
        propertyModality: Modality,
    ): FirPropertyAccessor {
        val getterFlags = if (proto.hasGetterFlags()) proto.getterFlags else defaultAccessorFlags
        val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(getterFlags))
        val accessorModality = ProtoEnumFlags.modality(Flags.MODALITY.get(getterFlags))
        val effectiveVisibility = visibility.toEffectiveVisibility(classSymbol)
        return if (Flags.IS_NOT_DEFAULT.get(getterFlags)) {
            buildPropertyAccessor {
                moduleData = c.moduleData
                origin = FirDeclarationOrigin.Library
                this.returnTypeRef = returnTypeRef
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                isGetter = true
                status = FirResolvedDeclarationStatusImpl(visibility, accessorModality, effectiveVisibility).apply {
                    isInline = Flags.IS_INLINE_ACCESSOR.get(getterFlags)
                    isExternal = Flags.IS_EXTERNAL_ACCESSOR.get(getterFlags)
                }
                this.symbol = FirPropertyAccessorSymbol()
                dispatchReceiverType = c.dispatchReceiver
                this.propertySymbol = propertySymbol
            }.apply {
                versionRequirementsTable = c.versionRequirementTable
            }
        } else {
            FirDefaultPropertyGetter(
                null,
                c.moduleData,
                FirDeclarationOrigin.Library,
                returnTypeRef,
                visibility,
                propertySymbol,
                propertyModality,
                effectiveVisibility
            )
        }.apply {
            (annotations as MutableList<FirAnnotation>) +=
                c.annotationDeserializer.loadPropertyGetterAnnotations(
                    c.containerSource, proto, local.nameResolver, local.typeTable, getterFlags
                )
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    private fun loadPropertySetter(
        proto: ProtoBuf.Property,
        classProto: ProtoBuf.Class? = null,
        classSymbol: FirClassSymbol<*>?,
        defaultAccessorFlags: Int,
        returnTypeRef: FirTypeRef,
        propertySymbol: FirPropertySymbol,
        local: FirDeserializationContext,
        propertyModality: Modality,
    ): FirPropertyAccessor {
        val setterFlags = if (proto.hasSetterFlags()) proto.setterFlags else defaultAccessorFlags
        val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(setterFlags))
        val accessorModality = ProtoEnumFlags.modality(Flags.MODALITY.get(setterFlags))
        val effectiveVisibility = visibility.toEffectiveVisibility(classSymbol)
        return if (Flags.IS_NOT_DEFAULT.get(setterFlags)) {
            buildPropertyAccessor {
                moduleData = c.moduleData
                origin = FirDeclarationOrigin.Library
                this.returnTypeRef = FirImplicitUnitTypeRef(source)
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                isGetter = false
                status = FirResolvedDeclarationStatusImpl(visibility, accessorModality, effectiveVisibility).apply {
                    isInline = Flags.IS_INLINE_ACCESSOR.get(setterFlags)
                    isExternal = Flags.IS_EXTERNAL_ACCESSOR.get(setterFlags)
                }
                this.symbol = FirPropertyAccessorSymbol()
                dispatchReceiverType = c.dispatchReceiver
                valueParameters += local.memberDeserializer.valueParameters(
                    listOf(proto.setterValueParameter),
                    proto,
                    AbstractAnnotationDeserializer.CallableKind.PROPERTY_SETTER,
                    classProto
                )
                this.propertySymbol = propertySymbol
            }.apply {
                versionRequirementsTable = c.versionRequirementTable
            }
        } else {
            FirDefaultPropertySetter(
                null,
                c.moduleData,
                FirDeclarationOrigin.Library,
                returnTypeRef,
                visibility,
                propertySymbol,
                propertyModality,
                effectiveVisibility
            )
        }.apply {
            (annotations as MutableList<FirAnnotation>) +=
                c.annotationDeserializer.loadPropertySetterAnnotations(
                    c.containerSource, proto, local.nameResolver, local.typeTable, setterFlags
                )
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    fun loadProperty(
        proto: ProtoBuf.Property,
        classProto: ProtoBuf.Class? = null,
        classSymbol: FirClassSymbol<*>? = null
    ): FirProperty {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)
        val callableName = c.nameResolver.getName(proto.name)
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = FirPropertySymbol(callableId)
        val local = c.childContext(proto.typeParameterList, containingDeclarationSymbol = symbol)

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

        val propertyModality = ProtoEnumFlags.modality(Flags.MODALITY.get(flags))

        val isVar = Flags.IS_VAR.get(flags)
        return buildProperty {
            moduleData = c.moduleData
            origin = FirDeclarationOrigin.Library
            this.returnTypeRef = returnTypeRef
            receiverTypeRef = proto.receiverType(c.typeTable)?.toTypeRef(local).apply {
                annotations += receiverAnnotations
            }
            name = callableName
            this.isVar = isVar
            this.symbol = symbol
            dispatchReceiverType = c.dispatchReceiver
            isLocal = false
            val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags))
            status = FirResolvedDeclarationStatusImpl(visibility, propertyModality, visibility.toEffectiveVisibility(classSymbol)).apply {
                isExpect = Flags.IS_EXPECT_PROPERTY.get(flags)
                isActual = false
                isOverride = false
                isConst = Flags.IS_CONST.get(flags)
                isLateInit = Flags.IS_LATEINIT.get(flags)
                isExternal = Flags.IS_EXTERNAL_PROPERTY.get(flags)
            }

            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            annotations +=
                c.annotationDeserializer.loadPropertyAnnotations(c.containerSource, proto, classProto, local.nameResolver, local.typeTable)
            annotations +=
                c.annotationDeserializer.loadPropertyBackingFieldAnnotations(
                    c.containerSource, proto, local.nameResolver, local.typeTable
                )
            annotations +=
                c.annotationDeserializer.loadPropertyDelegatedFieldAnnotations(
                    c.containerSource, proto, local.nameResolver, local.typeTable
                )
            if (hasGetter) {
                this.getter = loadPropertyGetter(
                    proto,
                    classSymbol,
                    defaultAccessorFlags,
                    returnTypeRef,
                    symbol,
                    local,
                    propertyModality
                )
            }
            if (Flags.HAS_SETTER.get(flags)) {
                this.setter = loadPropertySetter(
                    proto,
                    classProto,
                    classSymbol,
                    defaultAccessorFlags,
                    returnTypeRef,
                    symbol,
                    local,
                    propertyModality
                )
            }
            this.containerSource = c.containerSource
            this.initializer = c.constDeserializer.loadConstant(proto, symbol.callableId, c.nameResolver)
            deprecation = annotations.getDeprecationInfosFromAnnotations(c.session.languageVersionSettings.apiVersion, false)
        }.apply {
            versionRequirementsTable = c.versionRequirementTable
        }
    }

    fun loadFunction(
        proto: ProtoBuf.Function,
        classProto: ProtoBuf.Class? = null,
        classSymbol: FirClassSymbol<*>? = null,
        // TODO: introduce the similar changes for the other deserialized entities
        deserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
    ): FirSimpleFunction {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)

        val receiverAnnotations = if (proto.hasReceiver()) {
            c.annotationDeserializer.loadExtensionReceiverParameterAnnotations(
                c.containerSource, proto, c.nameResolver, c.typeTable, AbstractAnnotationDeserializer.CallableKind.OTHERS
            )
        } else {
            emptyList()
        }

        val callableName = c.nameResolver.getName(proto.name)
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = FirNamedFunctionSymbol(callableId)
        val local = c.childContext(proto.typeParameterList, containingDeclarationSymbol = symbol)

        val simpleFunction = buildSimpleFunction {
            moduleData = c.moduleData
            origin = deserializationOrigin
            returnTypeRef = proto.returnType(local.typeTable).toTypeRef(local)
            receiverTypeRef = proto.receiverType(local.typeTable)?.toTypeRef(local).apply {
                annotations += receiverAnnotations
            }
            name = callableName
            val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags))
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                ProtoEnumFlags.modality(Flags.MODALITY.get(flags)),
                visibility.toEffectiveVisibility(classSymbol)
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
            dispatchReceiverType = c.dispatchReceiver
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
            deprecation = annotations.getDeprecationInfosFromAnnotations(c.session.languageVersionSettings.apiVersion, false)
            this.containerSource = c.containerSource
        }.apply {
            versionRequirementsTable = c.versionRequirementTable
        }
        if (proto.hasContract()) {
            val contractDeserializer = if (proto.typeParameterList.isEmpty()) this.contractDeserializer else FirContractDeserializer(local)
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
        val local = c.childContext(emptyList(), containingDeclarationSymbol = symbol)
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
            moduleData = c.moduleData
            origin = FirDeclarationOrigin.Library
            returnTypeRef = delegatedSelfType
            val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags))
            val isInner = classBuilder.status.isInner
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                visibility.toEffectiveVisibility(classBuilder.symbol)
            ).apply {
                isExpect = Flags.IS_EXPECT_FUNCTION.get(flags)
                isActual = false
                isOverride = false
                this.isInner = isInner
            }
            this.symbol = symbol
            dispatchReceiverType =
                if (!isInner) null
                else with(c) {
                    ClassId(packageFqName, relativeClassName.parent(), false).defaultType(outerTypeParameters)
                }
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
            containerSource = c.containerSource
            deprecation = annotations.getDeprecationInfosFromAnnotations(c.session.languageVersionSettings.apiVersion, false)
        }.build().apply {
            containingClassForStaticMemberAttr = c.dispatchReceiver!!.lookupTag
            versionRequirementsTable = c.versionRequirementTable
        }
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
                moduleData = c.moduleData
                origin = FirDeclarationOrigin.Library
                returnTypeRef = proto.type(c.typeTable).toTypeRef(c)
                this.name = name
                symbol = FirValueParameterSymbol(name)
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
            }.apply {
                versionRequirementsTable = c.versionRequirementTable
            }
        }.toList()
    }

    private fun ProtoBuf.Type.toTypeRef(context: FirDeserializationContext): FirTypeRef {
        return buildResolvedTypeRef {
            annotations += context.annotationDeserializer.loadTypeAnnotations(this@toTypeRef, context.nameResolver)
            val attributes = annotations.computeTypeAttributes(context.session)
            type = context.typeDeserializer.type(this@toTypeRef, attributes)
        }
    }
}
