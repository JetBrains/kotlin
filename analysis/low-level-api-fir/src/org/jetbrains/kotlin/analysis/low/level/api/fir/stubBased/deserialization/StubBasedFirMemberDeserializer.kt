/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class StubBasedFirDeserializationContext(
    val moduleData: FirModuleData,
    val packageFqName: FqName,
    val relativeClassName: FqName?,
    val typeDeserializer: StubBasedFirTypeDeserializer,
    val annotationDeserializer: StubBasedAbstractAnnotationDeserializer,
    val constDeserializer: FirConstDeserializer,
    val containerSource: DeserializedContainerSource?,
    val outerClassSymbol: FirRegularClassSymbol?,
    val outerTypeParameters: List<FirTypeParameterSymbol>
) {
    val session: FirSession = moduleData.session

    val allTypeParameters: List<FirTypeParameterSymbol> =
        typeDeserializer.ownTypeParameters + outerTypeParameters

    fun childContext(
        owner: KtTypeParameterListOwner,
        relativeClassName: FqName? = this.relativeClassName,
        containerSource: DeserializedContainerSource? = this.containerSource,
        outerClassSymbol: FirRegularClassSymbol? = this.outerClassSymbol,
        annotationDeserializer: StubBasedAbstractAnnotationDeserializer = this.annotationDeserializer,
        constDeserializer: FirConstDeserializer = this.constDeserializer,
        capturesTypeParameters: Boolean = true,
        containingDeclarationSymbol: FirBasedSymbol<*>? = this.outerClassSymbol
    ): StubBasedFirDeserializationContext = StubBasedFirDeserializationContext(
        moduleData,
        packageFqName,
        relativeClassName,
        StubBasedFirTypeDeserializer(
            moduleData,
            annotationDeserializer,
            owner,
            typeDeserializer,
            containingDeclarationSymbol
        ),
        annotationDeserializer,
        constDeserializer,
        containerSource,
        outerClassSymbol,
        if (capturesTypeParameters) allTypeParameters else emptyList()
    )

    val memberDeserializer: StubBasedFirMemberDeserializer = StubBasedFirMemberDeserializer(this)
    val dispatchReceiver = relativeClassName?.let { ClassId(packageFqName, it, false).defaultType(allTypeParameters) }

    companion object {

        fun createForClass(
            classId: ClassId,
            classOrObject: KtClassOrObject,
            moduleData: FirModuleData,
            annotationDeserializer: StubBasedAbstractAnnotationDeserializer,
            constDeserializer: FirConstDeserializer,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: FirRegularClassSymbol
        ): StubBasedFirDeserializationContext = createRootContext(
            moduleData,
            annotationDeserializer,
            constDeserializer,
            classId.packageFqName,
            classId.relativeClassName,
            classOrObject,
            containerSource,
            outerClassSymbol,
            outerClassSymbol
        )

        fun createRootContext(
            moduleData: FirModuleData,
            annotationDeserializer: StubBasedAbstractAnnotationDeserializer,
            constDeserializer: FirConstDeserializer,
            packageFqName: FqName,
            relativeClassName: FqName?,
            owner: KtTypeParameterListOwner,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: FirRegularClassSymbol?,
            containingDeclarationSymbol: FirBasedSymbol<*>?
        ): StubBasedFirDeserializationContext = StubBasedFirDeserializationContext(
            moduleData,
            packageFqName,
            relativeClassName,
            StubBasedFirTypeDeserializer(
                moduleData,
                annotationDeserializer,
                owner,
                null,
                containingDeclarationSymbol
            ),
            annotationDeserializer,
            constDeserializer,
            containerSource,
            outerClassSymbol,
            emptyList()
        )

        fun createRootContext(
            session: FirSession,
            moduleData: FirModuleData,
            callableId: CallableId,
            parameterListOwner: KtTypeParameterListOwner,
            symbol: FirBasedSymbol<*>
        ): StubBasedFirDeserializationContext = createRootContext(
            moduleData,
            JvmAnnotationsDeserializer(session),
            FirConstDeserializer(session),
            callableId.packageName,
            callableId.className,
            parameterListOwner,
            JvmFromStubDecompilerSource(callableId.packageName),
            null,
            symbol
        )
    }
}

class StubBasedFirMemberDeserializer(private val c: StubBasedFirDeserializationContext) {

    fun loadTypeAlias(typeAlias: KtTypeAlias, aliasSymbol: FirTypeAliasSymbol): FirTypeAlias {
        val name = typeAlias.nameAsSafeName
        val local = c.childContext(typeAlias, containingDeclarationSymbol = aliasSymbol)
        return buildTypeAlias {
            moduleData = c.moduleData
            origin = FirDeclarationOrigin.Library
            this.name = name
            val visibility = typeAlias.visibility
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                visibility.toEffectiveVisibility(owner = null)
            ).apply {
                isExpect = typeAlias.hasModifier(KtTokens.EXPECT_KEYWORD)
                isActual = false
            }

            annotations += c.annotationDeserializer.loadTypeAliasAnnotations(typeAlias)
            symbol = aliasSymbol
            expandedTypeRef = typeAlias.getTypeReference()?.toTypeRef(local) ?: error("Type alias doesn't have type reference $typeAlias")
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
        }.apply {
            sourceElement = c.containerSource
        }
    }

    private fun loadPropertyGetter(
        getter: KtPropertyAccessor,
        classSymbol: FirClassSymbol<*>?,
        returnTypeRef: FirTypeRef,
        propertySymbol: FirPropertySymbol,
        propertyModality: Modality,
    ): FirPropertyAccessor {
        val visibility = getter.visibility
        val accessorModality = getter.modality
        val effectiveVisibility = visibility.toEffectiveVisibility(classSymbol)
        return if (getter.hasBody()) {
            buildPropertyAccessor {
                moduleData = c.moduleData
                origin = FirDeclarationOrigin.Library
                this.returnTypeRef = returnTypeRef
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                isGetter = true
                status = FirResolvedDeclarationStatusImpl(visibility, accessorModality, effectiveVisibility).apply {
                    isInline = getter.hasModifier(KtTokens.INLINE_KEYWORD)
                    isExternal = getter.hasModifier(KtTokens.EXTERNAL_KEYWORD)
                }
                this.symbol = FirPropertyAccessorSymbol()
                dispatchReceiverType = c.dispatchReceiver
                this.propertySymbol = propertySymbol
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
            replaceAnnotations(
                c.annotationDeserializer.loadPropertyGetterAnnotations(
                    getter
                )
            )
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    private fun loadPropertySetter(
        setter: KtPropertyAccessor,
        classOrObject: KtClassOrObject? = null,
        classSymbol: FirClassSymbol<*>?,
        returnTypeRef: FirTypeRef,
        propertySymbol: FirPropertySymbol,
        local: StubBasedFirDeserializationContext,
        propertyModality: Modality,
    ): FirPropertyAccessor {
        val visibility = setter.visibility
        val accessorModality = setter.modality
        val effectiveVisibility = visibility.toEffectiveVisibility(classSymbol)
        return if (setter.hasBody()) {
            buildPropertyAccessor {
                moduleData = c.moduleData
                origin = FirDeclarationOrigin.Library
                this.returnTypeRef = FirImplicitUnitTypeRef(source)
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                isGetter = false
                status = FirResolvedDeclarationStatusImpl(visibility, accessorModality, effectiveVisibility).apply {
                    isInline = setter.hasModifier(KtTokens.INLINE_KEYWORD)
                    isExternal = setter.hasModifier(KtTokens.EXTERNAL_KEYWORD)
                }
                this.symbol = FirPropertyAccessorSymbol()
                dispatchReceiverType = c.dispatchReceiver
                valueParameters += local.memberDeserializer.valueParameters(
                    setter.valueParameters,
                    symbol,
                    StubBasedAbstractAnnotationDeserializer.CallableKind.PROPERTY_SETTER,
                    classOrObject
                )
                this.propertySymbol = propertySymbol
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
            replaceAnnotations(
                c.annotationDeserializer.loadPropertySetterAnnotations(
                    setter
                )
            )
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    fun loadProperty(
        property: KtProperty,
        classOrObject: KtClassOrObject? = null,
        classSymbol: FirClassSymbol<*>? = null,
        existingSymbol: FirPropertySymbol? = null
    ): FirProperty {
        val callableName = property.nameAsSafeName
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = existingSymbol ?: FirPropertySymbol(callableId)
        val local = c.childContext(property, containingDeclarationSymbol = symbol)

        val returnTypeRef = property.typeReference?.toTypeRef(local) ?: error("Property doesn't have type reference, $property")

        val getter = property.getter
        val receiverAnnotations = if (getter != null && property.receiverTypeReference != null) {
            c.annotationDeserializer.loadExtensionReceiverParameterAnnotations(
                property, StubBasedAbstractAnnotationDeserializer.CallableKind.PROPERTY_GETTER
            )
        } else {
            emptyList()
        }

        val propertyModality = property.modality

        val isVar = property.isVar
        return buildProperty {
            moduleData = c.moduleData
            origin = FirDeclarationOrigin.Library
            this.returnTypeRef = returnTypeRef
            receiverParameter = property.receiverTypeReference?.toTypeRef(local)?.let { receiverType ->
                buildReceiverParameter {
                    typeRef = receiverType
                    annotations += receiverAnnotations
                }
            }

            name = callableName
            this.isVar = isVar
            this.symbol = symbol
            dispatchReceiverType = c.dispatchReceiver
            isLocal = false
            val visibility = property.visibility
            status = FirResolvedDeclarationStatusImpl(visibility, propertyModality, visibility.toEffectiveVisibility(classSymbol)).apply {
                isExpect = property.hasExpectModifier()
                isActual = false
                isOverride = false
                isConst = property.hasModifier(KtTokens.CONST_KEYWORD)
                isLateInit = property.hasModifier(KtTokens.LATEINIT_KEYWORD)
                isExternal = property.hasModifier(KtTokens.EXTERNAL_KEYWORD)
            }

            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            annotations +=
                c.annotationDeserializer.loadPropertyAnnotations(property, classOrObject)
            annotations +=
                c.annotationDeserializer.loadPropertyBackingFieldAnnotations(
                    property
                )
            annotations +=
                c.annotationDeserializer.loadPropertyDelegatedFieldAnnotations(
                    property
                )
            if (getter != null) {
                this.getter = loadPropertyGetter(
                    getter,
                    classSymbol,
                    returnTypeRef,
                    symbol,
                    propertyModality
                )
            }
            val setter = property.setter
            if (setter != null) {
                this.setter = loadPropertySetter(
                    setter,
                    classOrObject,
                    classSymbol,
                    returnTypeRef,
                    symbol,
                    local,
                    propertyModality
                )
            }
            this.containerSource = c.containerSource
            this.initializer = c.constDeserializer.loadConstant(property, symbol.callableId)
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(c.session, fromJava = false)

            property.contextReceivers.mapNotNull { it.typeReference() }.mapTo(contextReceivers, ::loadContextReceiver)
        }
    }

    private fun loadContextReceiver(typeReference: KtTypeReference): FirContextReceiver {
        val typeRef = typeReference.toTypeRef(c)
        return buildContextReceiver {
            val type = typeRef.coneType
            this.labelNameFromTypeRef = (type as? ConeLookupTagBasedType)?.lookupTag?.name
            this.typeRef = typeRef
        }
    }

    internal fun createContextReceiversForClass(classOrObject: KtClassOrObject): List<FirContextReceiver> =
        classOrObject.contextReceivers.mapNotNull { it.typeReference() }.map(::loadContextReceiver)

    fun loadFunction(
        function: KtNamedFunction,
        classOrObject: KtClassOrObject? = null,
        classSymbol: FirClassSymbol<*>? = null,
        session: FirSession,
        existingSymbol: FirNamedFunctionSymbol? = null
    ): FirSimpleFunction {
        val receiverAnnotations = if (function.receiverTypeReference != null) {
            c.annotationDeserializer.loadExtensionReceiverParameterAnnotations(
                function, StubBasedAbstractAnnotationDeserializer.CallableKind.OTHERS
            )
        } else {
            emptyList()
        }

        val callableName = function.nameAsSafeName
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = existingSymbol ?: FirNamedFunctionSymbol(callableId)
        val local = c.childContext(function, containingDeclarationSymbol = symbol)

        val simpleFunction = buildSimpleFunction {
            moduleData = c.moduleData
            origin = FirDeclarationOrigin.Library
            returnTypeRef = function.typeReference?.toTypeRef(local) ?: session.builtinTypes.unitType
            receiverParameter = function.receiverTypeReference?.toTypeRef(local)?.let { receiverType ->
                buildReceiverParameter {
                    typeRef = receiverType
                    annotations += receiverAnnotations
                }
            }

            name = callableName
            val visibility = function.visibility
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                function.modality,
                visibility.toEffectiveVisibility(classSymbol)
            ).apply {
                isExpect = function.hasExpectModifier()
                isActual = false
                isOverride = false
                isOperator = function.hasModifier(KtTokens.OPERATOR_KEYWORD)
                isInfix = function.hasModifier(KtTokens.INFIX_KEYWORD)
                isInline = function.hasModifier(KtTokens.INLINE_KEYWORD)
                isTailRec = function.hasModifier(KtTokens.TAILREC_KEYWORD)
                isExternal = function.hasModifier(KtTokens.EXTERNAL_KEYWORD)
                isSuspend = function.hasModifier(KtTokens.SUSPEND_KEYWORD)
            }
            this.symbol = symbol
            dispatchReceiverType = c.dispatchReceiver
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            valueParameters += local.memberDeserializer.valueParameters(
                function.valueParameters,
                symbol,
                StubBasedAbstractAnnotationDeserializer.CallableKind.OTHERS,
                classOrObject
            )
            annotations +=
                c.annotationDeserializer.loadFunctionAnnotations(function)
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(c.session, fromJava = false)
            this.containerSource = c.containerSource

            function.contextReceivers.mapNotNull { it.typeReference() }.mapTo(contextReceivers, ::loadContextReceiver)
        }
        //todo contracts
//        if (function.hasContract()) {
//            val contractDeserializer = if (function.typeParameters.isEmpty()) this.contractDeserializer else FirContractDeserializer(local)
//            val contractDescription = contractDeserializer.loadContract(function.contract, simpleFunction)
//            if (contractDescription != null) {
//                simpleFunction.replaceContractDescription(contractDescription)
//            }
//        }
        return simpleFunction
    }

    fun loadConstructor(
        constructor: KtConstructor<*>,
        classOrObject: KtClassOrObject,
        classBuilder: FirRegularClassBuilder
    ): FirConstructor {
        val relativeClassName = c.relativeClassName!!
        val callableId = CallableId(c.packageFqName, relativeClassName, relativeClassName.shortName())
        val symbol = FirConstructorSymbol(callableId)
        val local = c.childContext(constructor, containingDeclarationSymbol = symbol)
        val isPrimary = constructor is KtPrimaryConstructor

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
            val visibility = constructor.visibility
            val isInner = classBuilder.status.isInner
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                visibility.toEffectiveVisibility(classBuilder.symbol)
            ).apply {
                isExpect = constructor.hasExpectModifier() || classOrObject.hasExpectModifier()
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
                constructor.valueParameters,
                symbol,
                StubBasedAbstractAnnotationDeserializer.CallableKind.OTHERS,
                classOrObject,
                addDefaultValue = classBuilder.symbol.classId == StandardClassIds.Enum
            )
            annotations +=
                c.annotationDeserializer.loadConstructorAnnotations(constructor)
            containerSource = c.containerSource
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(c.session, fromJava = false)

            contextReceivers.addAll(createContextReceiversForClass(classOrObject))
        }.build().apply {
            containingClassForStaticMemberAttr = c.dispatchReceiver!!.lookupTag
        }
    }

    private fun valueParameters(
        valueParameters: List<KtParameter>,
        functionSymbol: FirFunctionSymbol<*>,
        callableKind: StubBasedAbstractAnnotationDeserializer.CallableKind,
        classOrObject: KtClassOrObject?,
        addDefaultValue: Boolean = false
    ): List<FirValueParameter> {
        return valueParameters.mapIndexed { index, ktParameter ->
            val name = ktParameter.nameAsSafeName
            buildValueParameter {
                moduleData = c.moduleData
                this.containingFunctionSymbol = functionSymbol
                origin = FirDeclarationOrigin.Library
                returnTypeRef =
                    ktParameter.typeReference?.toTypeRef(c) ?: error("KtParameter $ktParameter doesn't have type, $functionSymbol")
                isVararg = ktParameter.isVarArg
                if (isVararg) {
                    returnTypeRef = returnTypeRef.withReplacedReturnType(returnTypeRef.coneType.createOutArrayType())
                }
                this.name = name
                symbol = FirValueParameterSymbol(name)
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

                defaultValue = if (ktParameter.hasDefaultValue()) {
                    buildExpressionStub()
                } else null
                if (addDefaultValue) {
                    defaultValue = buildExpressionStub()
                }
                isCrossinline = ktParameter.hasModifier(KtTokens.CROSSINLINE_KEYWORD)
                isNoinline = ktParameter.hasModifier(KtTokens.NOINLINE_KEYWORD)
                annotations += c.annotationDeserializer.loadValueParameterAnnotations(
                    ktParameter,
                    classOrObject,
                    callableKind,
                    index,
                )
            }
        }.toList()
    }

    private fun KtTypeReference.toTypeRef(context: StubBasedFirDeserializationContext): FirTypeRef =
        context.typeDeserializer.typeRef(this)
}
