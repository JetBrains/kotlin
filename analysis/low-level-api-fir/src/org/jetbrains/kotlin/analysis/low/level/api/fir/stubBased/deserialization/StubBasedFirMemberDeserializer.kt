/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.copyWithNewSourceKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.deserialization.toLazyEffectiveVisibility
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.transformers.setLazyPublishedVisibility
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class StubBasedFirDeserializationContext(
    val moduleData: FirModuleData,
    val packageFqName: FqName,
    val relativeClassName: FqName?,
    val typeDeserializer: StubBasedFirTypeDeserializer,
    val annotationDeserializer: StubBasedAnnotationDeserializer,
    val containerSource: DeserializedContainerSource?,
    val outerClassSymbol: FirRegularClassSymbol?,
    val outerTypeParameters: List<FirTypeParameterSymbol>,
    val initialOrigin: FirDeclarationOrigin,
    val classLikeDeclaration: KtClassLikeDeclaration? = null
) {
    val session: FirSession get() = moduleData.session

    val allTypeParameters: List<FirTypeParameterSymbol> =
        typeDeserializer.ownTypeParameters + outerTypeParameters

    fun childContext(
        owner: KtTypeParameterListOwner,
        relativeClassName: FqName? = this.relativeClassName,
        containerSource: DeserializedContainerSource? = this.containerSource,
        outerClassSymbol: FirRegularClassSymbol? = this.outerClassSymbol,
        annotationDeserializer: StubBasedAnnotationDeserializer = this.annotationDeserializer,
        capturesTypeParameters: Boolean = true,
        containingDeclarationSymbol: FirBasedSymbol<*>? = outerClassSymbol
    ): StubBasedFirDeserializationContext = StubBasedFirDeserializationContext(
        moduleData = moduleData,
        packageFqName = packageFqName,
        relativeClassName = relativeClassName,
        typeDeserializer = StubBasedFirTypeDeserializer(
            moduleData,
            annotationDeserializer,
            typeDeserializer,
            containingDeclarationSymbol,
            owner,
            initialOrigin
        ),
        annotationDeserializer = annotationDeserializer,
        containerSource = containerSource,
        outerClassSymbol = outerClassSymbol,
        outerTypeParameters = if (capturesTypeParameters) allTypeParameters else emptyList(),
        initialOrigin = initialOrigin
    )

    fun withClassLikeDeclaration(
        classLikeDeclaration: KtClassLikeDeclaration,
    ): StubBasedFirDeserializationContext = StubBasedFirDeserializationContext(
        moduleData = moduleData,
        packageFqName = packageFqName,
        relativeClassName = relativeClassName,
        typeDeserializer = typeDeserializer,
        annotationDeserializer = annotationDeserializer,
        containerSource = containerSource,
        outerClassSymbol = outerClassSymbol,
        outerTypeParameters = outerTypeParameters,
        initialOrigin = initialOrigin,
        classLikeDeclaration = classLikeDeclaration,
    )

    val memberDeserializer: StubBasedFirMemberDeserializer = StubBasedFirMemberDeserializer(this, initialOrigin)
    val dispatchReceiver = relativeClassName?.let { ClassId(packageFqName, it, isLocal = false).defaultType(allTypeParameters) }

    companion object {

        fun createForClass(
            classId: ClassId,
            classOrObject: KtClassOrObject,
            moduleData: FirModuleData,
            annotationDeserializer: StubBasedAnnotationDeserializer,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: FirRegularClassSymbol,
            initialOrigin: FirDeclarationOrigin
        ): StubBasedFirDeserializationContext = createRootContext(
            moduleData,
            annotationDeserializer,
            classId.packageFqName,
            classId.relativeClassName,
            classOrObject,
            containerSource,
            outerClassSymbol,
            outerClassSymbol,
            initialOrigin
        )

        fun createRootContext(
            moduleData: FirModuleData,
            annotationDeserializer: StubBasedAnnotationDeserializer,
            packageFqName: FqName,
            relativeClassName: FqName?,
            owner: KtTypeParameterListOwner,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: FirRegularClassSymbol?,
            containingDeclarationSymbol: FirBasedSymbol<*>?,
            initialOrigin: FirDeclarationOrigin
        ): StubBasedFirDeserializationContext = StubBasedFirDeserializationContext(
            moduleData,
            packageFqName,
            relativeClassName,
            StubBasedFirTypeDeserializer(
                moduleData,
                annotationDeserializer,
                parent = null,
                containingDeclarationSymbol,
                owner,
                initialOrigin
            ),
            annotationDeserializer,
            containerSource,
            outerClassSymbol,
            outerTypeParameters = emptyList(),
            initialOrigin
        )

        fun createRootContext(
            session: FirSession,
            moduleData: FirModuleData,
            callableId: CallableId,
            parameterListOwner: KtTypeParameterListOwner,
            symbol: FirBasedSymbol<*>,
            initialOrigin: FirDeclarationOrigin,
            containerSource: DeserializedContainerSource?,
        ): StubBasedFirDeserializationContext {
            return createRootContext(
                moduleData,
                StubBasedAnnotationDeserializer(session),
                callableId.packageName,
                callableId.className,
                parameterListOwner,
                containerSource = containerSource,
                outerClassSymbol = null,
                symbol,
                initialOrigin
            )
        }
    }
}

internal class StubBasedFirMemberDeserializer(
    private val c: StubBasedFirDeserializationContext,
    private val initialOrigin: FirDeclarationOrigin
) {

    fun loadTypeAlias(typeAlias: KtTypeAlias, aliasSymbol: FirTypeAliasSymbol, scopeProvider: FirScopeProvider): FirTypeAlias {
        val name = typeAlias.nameAsSafeName
        val local = c.childContext(typeAlias, containingDeclarationSymbol = aliasSymbol)
        return buildTypeAlias {
            source = KtRealPsiSourceElement(typeAlias)
            moduleData = c.moduleData
            origin = initialOrigin
            this.scopeProvider = scopeProvider
            this.name = name
            val visibility = typeAlias.visibility
            status = FirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                Modality.FINAL,
                visibility.toLazyEffectiveVisibility(owner = null)
            ).apply {
                isExpect = typeAlias.hasModifier(KtTokens.EXPECT_KEYWORD)
                isActual = false
            }

            annotations += c.annotationDeserializer.loadAnnotations(typeAlias)
            symbol = aliasSymbol
            expandedTypeRef = typeAlias.getTypeReference()?.toTypeRef(local)
                ?: errorWithAttachment("Type alias doesn't have type reference") {
                    withPsiEntry("property", typeAlias)
                }
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(c.session, fromJava = false)
        }.apply {
            sourceElement = c.containerSource
        }
    }

    private fun loadPropertyGetter(
        getter: KtPropertyAccessor,
        classSymbol: FirClassSymbol<*>?,
        returnTypeRef: FirTypeRef,
        propertySymbol: FirPropertySymbol,
    ): FirPropertyAccessor {
        val visibility = getter.visibility
        val accessorModality = getter.modality
        return buildPropertyAccessor {
            source = KtRealPsiSourceElement(getter)
            moduleData = c.moduleData
            origin = initialOrigin
            this.returnTypeRef = returnTypeRef
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            isGetter = true
            status = FirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                accessorModality,
                visibility.toLazyEffectiveVisibility(classSymbol)
            ).apply {
                isInline = getter.hasModifier(KtTokens.INLINE_KEYWORD)
                isExternal = getter.hasModifier(KtTokens.EXTERNAL_KEYWORD)
            }
            this.symbol = FirPropertyAccessorSymbol()
            dispatchReceiverType = c.dispatchReceiver
            this.propertySymbol = propertySymbol
        }.apply {
            replaceAnnotations(
                c.annotationDeserializer.loadAnnotations(getter)
            )

            replaceDeprecationsProvider(getDeprecationsProvider(c.session))
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    private fun loadPropertySetter(
        setter: KtPropertyAccessor,
        classSymbol: FirClassSymbol<*>?,
        propertySymbol: FirPropertySymbol,
        local: StubBasedFirDeserializationContext,
    ): FirPropertyAccessor {
        val visibility = setter.visibility
        val accessorModality = setter.modality
        return buildPropertyAccessor {
            source = KtRealPsiSourceElement(setter)
            moduleData = c.moduleData
            origin = initialOrigin
            this.returnTypeRef = FirImplicitUnitTypeRef(source)
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            isGetter = false
            status = FirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                accessorModality,
                visibility.toLazyEffectiveVisibility(classSymbol)
            ).apply {
                isInline = setter.hasModifier(KtTokens.INLINE_KEYWORD)
                isExternal = setter.hasModifier(KtTokens.EXTERNAL_KEYWORD)
            }
            this.symbol = FirPropertyAccessorSymbol()
            dispatchReceiverType = c.dispatchReceiver
            valueParameters += local.memberDeserializer.valueParameters(
                setter.valueParameters,
                symbol
            )
            this.propertySymbol = propertySymbol
        }.apply {
            replaceAnnotations(
                c.annotationDeserializer.loadAnnotations(setter)
            )

            replaceDeprecationsProvider(getDeprecationsProvider(c.session))
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    fun loadProperty(
        property: KtProperty,
        classSymbol: FirClassSymbol<*>? = null,
        existingSymbol: FirPropertySymbol? = null
    ): FirProperty {
        val callableName = property.nameAsSafeName
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = existingSymbol ?: FirPropertySymbol(callableId)
        val local = c.childContext(property, containingDeclarationSymbol = symbol)

        val returnTypeRef = property.typeReference?.toTypeRef(local)
            ?: errorWithAttachment("Property doesn't have type reference") {
                withPsiEntry("property", property)
            }

        val getter = property.getter
        val receiverTypeReference = property.receiverTypeReference
        val receiverAnnotations = if (getter != null && receiverTypeReference != null) {
            c.annotationDeserializer.loadAnnotations(receiverTypeReference)
        } else {
            emptyList()
        }

        val propertyModality = property.modality

        val isVar = property.isVar
        return buildProperty {
            source = KtRealPsiSourceElement(property)
            moduleData = c.moduleData
            origin = initialOrigin
            this.returnTypeRef = returnTypeRef
            receiverParameter = receiverTypeReference?.toTypeRef(local)?.let { receiverType ->
                buildReceiverParameter {
                    typeRef = receiverType
                    annotations += receiverAnnotations
                    this.symbol = FirReceiverParameterSymbol()
                    moduleData = c.moduleData
                    origin = initialOrigin
                    containingDeclarationSymbol = symbol
                }
            }

            name = callableName
            this.isVar = isVar
            this.symbol = symbol
            dispatchReceiverType = c.dispatchReceiver
            isLocal = false
            val visibility = property.visibility
            val resolvedStatus = FirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                propertyModality,
                visibility.toLazyEffectiveVisibility(classSymbol)
            ).apply {
                isExpect = property.hasExpectModifier()
                isActual = false
                isOverride = false
                isConst = property.hasModifier(KtTokens.CONST_KEYWORD)
                isLateInit = property.hasModifier(KtTokens.LATEINIT_KEYWORD)
                isExternal = property.hasModifier(KtTokens.EXTERNAL_KEYWORD)
            }

            status = resolvedStatus

            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            val allAnnotations = c.annotationDeserializer.loadAnnotations(property)
            annotations += allAnnotations.filter { it.useSiteTarget == null }
            val backingFieldAnnotations =
                allAnnotations.filter { it.useSiteTarget == AnnotationUseSiteTarget.FIELD || it.useSiteTarget == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD }
            backingField = FirDefaultPropertyBackingField(
                c.moduleData,
                initialOrigin,
                source = property.toKtPsiSourceElement(KtFakeSourceElementKind.DefaultAccessor),
                backingFieldAnnotations.toMutableList(),
                returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                isVar,
                symbol,
                status,
            )

            this.getter = getter?.let {
                loadPropertyGetter(
                    getter,
                    classSymbol,
                    returnTypeRef,
                    symbol
                )
            } ?: FirDefaultPropertyGetter(
                source = source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor),
                moduleData = moduleData,
                origin = origin,
                propertyTypeRef = returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                propertySymbol = symbol,
                status = resolvedStatus,
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES,
            )

            val setter = property.setter
            this.setter = when {
                setter != null -> loadPropertySetter(setter, classSymbol, symbol, local)
                isVar -> FirDefaultPropertySetter(
                    source = source?.fakeElement(KtFakeSourceElementKind.DefaultAccessor),
                    moduleData = moduleData,
                    origin = origin,
                    propertyTypeRef = returnTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.DefaultAccessor),
                    propertySymbol = symbol,
                    status = resolvedStatus,
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES,
                )
                else -> null
            }

            this.containerSource = c.containerSource
            this.initializer = c.annotationDeserializer.loadConstant(
                property,
                symbol.callableId,
                isUnsigned = returnTypeRef.coneType.isUnsignedType
            )

            property.contextReceiverList?.contextReceivers()?.mapTo(contextParameters) {
                loadContextReceiver(it, symbol)
            }
            property.contextReceiverList?.contextParameters()?.mapTo(contextParameters) {
                loadContextReceiver(it, symbol)
            }
        }.apply {
            setLazyPublishedVisibility(c.session)
            this.getter?.setLazyPublishedVisibility(annotations, this, c.session)
            this.setter?.setLazyPublishedVisibility(annotations, this, c.session)

            replaceDeprecationsProvider(getDeprecationsProvider(c.session))
        }
    }

    private fun loadContextReceiver(contextReceiver: KtContextReceiver, containingDeclarationSymbol: FirBasedSymbol<*>): FirValueParameter {
        return buildValueParameter {
            this.source = KtRealPsiSourceElement(contextReceiver)
            this.moduleData = c.moduleData
            this.origin = initialOrigin
            this.name = SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
            this.symbol = FirValueParameterSymbol(name)
            this.returnTypeRef = contextReceiver.typeReference()?.toTypeRef(c) ?: errorWithAttachment("KtParameter doesn't have type") {
                withPsiEntry("contextReceiver", contextReceiver)
                withFirSymbolEntry("functionSymbol", containingDeclarationSymbol)
            }
            this.containingDeclarationSymbol = containingDeclarationSymbol
            this.valueParameterKind = FirValueParameterKind.LegacyContextReceiver
            this.resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
        }
    }

    private fun loadContextReceiver(parameter: KtParameter, containingDeclarationSymbol: FirBasedSymbol<*>): FirValueParameter {
        return buildValueParameter {
            this.source = KtRealPsiSourceElement(parameter)
            this.moduleData = c.moduleData
            this.origin = initialOrigin
            this.name = if (parameter.name == "_") SpecialNames.UNDERSCORE_FOR_UNUSED_VAR else parameter.nameAsSafeName
            this.symbol = FirValueParameterSymbol(name)
            this.returnTypeRef = parameter.typeReference?.toTypeRef(c) ?: errorWithAttachment("KtParameter doesn't have type") {
                withPsiEntry("ktParameter", parameter)
                withFirSymbolEntry("functionSymbol", containingDeclarationSymbol)
            }
            this.containingDeclarationSymbol = containingDeclarationSymbol
            this.valueParameterKind = FirValueParameterKind.ContextParameter
            this.resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
        }
    }

    internal fun createContextReceiversForClass(
        classOrObject: KtClassOrObject,
        containingDeclarationSymbol: FirBasedSymbol<*>,
    ): List<FirValueParameter> {
        return classOrObject.contextReceivers.mapNotNull { it.typeReference() }.map {
            buildValueParameter {
                this.source = KtRealPsiSourceElement(it)
                this.moduleData = c.moduleData
                this.origin = initialOrigin
                this.name = SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                this.symbol = FirValueParameterSymbol(name)
                this.returnTypeRef = it.toTypeRef(c)
                this.containingDeclarationSymbol = containingDeclarationSymbol
                this.valueParameterKind = FirValueParameterKind.ContextParameter
                this.resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            }
        }
    }

    fun loadFunction(
        function: KtNamedFunction,
        classSymbol: FirClassSymbol<*>? = null,
        session: FirSession,
        existingSymbol: FirNamedFunctionSymbol? = null
    ): FirSimpleFunction {
        val receiverAnnotations = if (function.receiverTypeReference != null) {
            c.annotationDeserializer.loadAnnotations(
                function
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
            origin = initialOrigin
            source = KtRealPsiSourceElement(function)
            returnTypeRef = function.typeReference?.toTypeRef(local) ?: session.builtinTypes.unitType
            receiverParameter = function.receiverTypeReference?.toTypeRef(local)?.let { receiverType ->
                buildReceiverParameter {
                    typeRef = receiverType
                    annotations += receiverAnnotations
                    this.symbol = FirReceiverParameterSymbol()
                    moduleData = c.moduleData
                    origin = initialOrigin
                    containingDeclarationSymbol = symbol
                }
            }

            name = callableName
            val visibility = function.visibility
            status = FirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                function.modality,
                visibility.toLazyEffectiveVisibility(classSymbol)
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
                symbol
            )
            annotations +=
                c.annotationDeserializer.loadAnnotations(function)
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(c.session, fromJava = false)
            this.containerSource = c.containerSource

            function.contextReceiverList?.contextReceivers()?.mapTo(contextParameters) { loadContextReceiver(it, symbol) }
            function.contextReceiverList?.contextParameters()?.mapTo(contextParameters) { loadContextReceiver(it, symbol) }
        }.apply {
            setLazyPublishedVisibility(c.session)
        }
        if (function.mayHaveContract()) {
            val resolvedDescription = StubBasedFirContractDeserializer(simpleFunction, local.typeDeserializer).loadContract(function)
            if (resolvedDescription != null) {
                simpleFunction.replaceContractDescription(resolvedDescription)
            }
        }
        return simpleFunction
    }

    @OptIn(SuspiciousFakeSourceCheck::class)
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
            coneType = ConeClassLikeTypeImpl(
                classBuilder.symbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
            source = KtFakePsiSourceElement(classOrObject, KtFakeSourceElementKind.ClassSelfTypeRef)
        }

        return if (isPrimary) {
            FirPrimaryConstructorBuilder()
        } else {
            FirConstructorBuilder()
        }.apply {
            moduleData = c.moduleData
            source = KtRealPsiSourceElement(constructor)
            origin = initialOrigin
            returnTypeRef = delegatedSelfType
            val visibility = constructor.visibility
            val isInner = classBuilder.status.isInner
            status = FirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                Modality.FINAL,
                visibility.toLazyEffectiveVisibility(classBuilder.symbol)
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
                    ClassId(packageFqName, relativeClassName.parent(), isLocal = false).defaultType(outerTypeParameters)
                }
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            this.typeParameters +=
                typeParameters.filterIsInstance<FirTypeParameter>()
                    .map { buildConstructedClassTypeParameterRef { this.symbol = it.symbol } }
            valueParameters += local.memberDeserializer.valueParameters(
                constructor.valueParameters,
                symbol,
                addDefaultValue = classBuilder.symbol.classId == StandardClassIds.Enum
            )
            annotations +=
                c.annotationDeserializer.loadAnnotations(constructor)
            containerSource = c.containerSource
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(c.session, fromJava = false)

            contextParameters.addAll(createContextReceiversForClass(classOrObject, symbol))
        }.build().apply {
            containingClassForStaticMemberAttr = c.dispatchReceiver!!.lookupTag
            setLazyPublishedVisibility(c.session)
        }
    }

    private fun valueParameters(
        valueParameters: List<KtParameter>,
        functionSymbol: FirFunctionSymbol<*>,
        addDefaultValue: Boolean = false
    ): List<FirValueParameter> {
        return valueParameters.map { ktParameter ->
            val name = ktParameter.nameAsSafeName
            buildValueParameter {
                source = KtRealPsiSourceElement(ktParameter)
                moduleData = c.moduleData
                this.containingDeclarationSymbol = functionSymbol
                origin = initialOrigin
                returnTypeRef =
                    ktParameter.typeReference?.toTypeRef(c)
                        ?: errorWithAttachment("KtParameter doesn't have type") {
                            withPsiEntry("ktParameter", ktParameter)
                            withFirSymbolEntry("functionSymbol", functionSymbol)
                        }
                isVararg = ktParameter.isVarArg
                if (isVararg) {
                    returnTypeRef = returnTypeRef.withReplacedReturnType(returnTypeRef.coneType.createOutArrayType())
                }
                this.name = name
                symbol = FirValueParameterSymbol(name)
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES

                defaultValue = if (ktParameter.hasDefaultValue() || addDefaultValue) {
                    buildExpressionStub()
                } else null
                isCrossinline = ktParameter.hasModifier(KtTokens.CROSSINLINE_KEYWORD)
                isNoinline = ktParameter.hasModifier(KtTokens.NOINLINE_KEYWORD)
                annotations += c.annotationDeserializer.loadAnnotations(
                    ktParameter
                )
            }
        }.toList()
    }

    private fun KtTypeReference.toTypeRef(context: StubBasedFirDeserializationContext): FirTypeRef =
        context.typeDeserializer.typeRef(this)

    fun loadEnumEntry(
        declaration: KtEnumEntry,
        symbol: FirRegularClassSymbol,
        classId: ClassId
    ): FirEnumEntry {
        val enumEntryName = declaration.name
            ?: errorWithAttachment("Enum entry doesn't provide name") {
                withPsiEntry("declaration", declaration)
            }

        val enumType = ConeClassLikeTypeImpl(symbol.toLookupTag(), ConeTypeProjection.EMPTY_ARRAY, false)
        val enumEntry = buildEnumEntry {
            source = KtRealPsiSourceElement(declaration)
            this.moduleData = c.moduleData
            this.origin = initialOrigin
            returnTypeRef = buildResolvedTypeRef { coneType = enumType }
            name = Name.identifier(enumEntryName)
            this.symbol = FirEnumEntrySymbol(CallableId(classId, name))
            this.status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            ).apply {
                isStatic = true
            }
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
        }.apply {
            containingClassForStaticMemberAttr = c.dispatchReceiver!!.lookupTag
        }
        return enumEntry
    }

    private fun Visibility.toLazyEffectiveVisibility(owner: FirClassLikeSymbol<*>?): Lazy<EffectiveVisibility> {
        return this.toLazyEffectiveVisibility(owner, c.session, forClass = false)
    }
}
