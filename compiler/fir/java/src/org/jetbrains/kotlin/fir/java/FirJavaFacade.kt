/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildEnumEntry
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.classId
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.enhancement.FirSignatureEnhancement
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.load.java.FakePureImplementationsProvider
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.types.Variance.INVARIANT
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirJavaFacadeForSource(
    session: FirSession,
    private val sourceModuleData: FirModuleData,
    classFinder: JavaClassFinder
) : FirJavaFacade(session, sourceModuleData.session.builtinTypes, classFinder) {
    override fun getModuleDataForClass(javaClass: JavaClass): FirModuleData {
        return sourceModuleData
    }
}

@ThreadSafeMutableState
abstract class FirJavaFacade(
    private val session: FirSession,
    private val builtinTypes: BuiltinTypes,
    private val classFinder: JavaClassFinder
) {
    companion object {
        val VALUE_METHOD_NAME = Name.identifier("value")
        private const val PACKAGE_INFO_CLASS_NAME = "package-info"
    }

    private val packageCache = session.firCachesFactory.createCache { fqName: FqName ->
        val knownClassNames: Set<String>? = knownClassNamesInPackage.getValue(fqName)
        classFinder.findPackage(
            fqName,
            mayHaveAnnotations = if (knownClassNames != null) PACKAGE_INFO_CLASS_NAME in knownClassNames else true
        )
    }
    private val knownClassNamesInPackage = session.firCachesFactory.createCache(classFinder::knownClassNamesInPackage)

    private val parentClassTypeParameterStackCache = mutableMapOf<FirRegularClassSymbol, JavaTypeParameterStack>()
    private val parentClassEffectiveVisibilityCache = mutableMapOf<FirRegularClassSymbol, EffectiveVisibility>()

    fun findClass(classId: ClassId, knownContent: ByteArray? = null): JavaClass? =
        classFinder.findClass(JavaClassFinder.Request(classId, knownContent))
            ?.takeIf { it.classId == classId && !it.hasMetadataAnnotation() }

    fun getPackage(fqName: FqName): FqName? =
        try {
            packageCache.getValue(fqName)?.fqName
        } catch (e: ProcessCanceledException) {
            null
        }

    fun hasTopLevelClassOf(classId: ClassId): Boolean {
        val knownNames = knownClassNamesInPackage.getValue(classId.packageFqName) ?: return true
        return classId.relativeClassName.topLevelName() in knownNames
    }

    abstract fun getModuleDataForClass(javaClass: JavaClass): FirModuleData

    private fun JavaTypeParameter.toFirTypeParameter(
        javaTypeParameterStack: JavaTypeParameterStack,
        containingDeclarationSymbol: FirBasedSymbol<*>,
        moduleData: FirModuleData,
    ): FirTypeParameter {
        return buildTypeParameter {
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Java
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            name = this@toFirTypeParameter.name
            symbol = FirTypeParameterSymbol()
            variance = INVARIANT
            isReified = false
            javaTypeParameterStack.addParameter(this@toFirTypeParameter, symbol)
            // TODO: should be lazy (in case annotations refer to the containing class)
            annotations.addFromJava(session, this@toFirTypeParameter, javaTypeParameterStack)
            this.containingDeclarationSymbol = containingDeclarationSymbol
            for (upperBound in this@toFirTypeParameter.upperBounds) {
                bounds += upperBound.toFirJavaTypeRef(session, javaTypeParameterStack)
            }
            if (bounds.isEmpty()) {
                bounds += buildResolvedTypeRef {
                    type = ConeFlexibleType(builtinTypes.anyType.type, builtinTypes.nullableAnyType.type)
                }
            }
        }
    }

    private fun List<JavaTypeParameter>.convertTypeParameters(
        stack: JavaTypeParameterStack,
        containingDeclarationSymbol: FirBasedSymbol<*>,
        moduleData: FirModuleData,
    ): List<FirTypeParameter> {
        return map { it.toFirTypeParameter(stack, containingDeclarationSymbol, moduleData) }
    }

    private fun JavaClass.hasMetadataAnnotation(): Boolean =
        annotations.any { it.classId?.asSingleFqName() == JvmAnnotationNames.METADATA_FQ_NAME }

    private class ValueParametersForAnnotationConstructor {
        val valueParameters: MutableMap<JavaMethod, FirJavaValueParameter> = linkedMapOf()
        var valueParameterForValue: Pair<JavaMethod, FirJavaValueParameter>? = null

        inline fun forEach(block: (JavaMethod, FirJavaValueParameter) -> Unit) {
            valueParameterForValue?.let { (javaMethod, firJavaValueParameter) -> block(javaMethod, firJavaValueParameter) }
            valueParameters.forEach { (javaMethod, firJavaValueParameter) -> block(javaMethod, firJavaValueParameter) }
        }
    }

    fun convertJavaClassToFir(
        classSymbol: FirRegularClassSymbol,
        parentClassSymbol: FirRegularClassSymbol?,
        javaClass: JavaClass,
    ): FirJavaClass {
        val classId = classSymbol.classId
        val javaTypeParameterStack = JavaTypeParameterStack()

        if (parentClassSymbol != null) {
            val parentStack = parentClassTypeParameterStackCache[parentClassSymbol]
                ?: (parentClassSymbol.fir as? FirJavaClass)?.javaTypeParameterStack
            if (parentStack != null) {
                javaTypeParameterStack.addStack(parentStack)
            }
        }
        parentClassTypeParameterStackCache[classSymbol] = javaTypeParameterStack
        val firJavaClass = createFirJavaClass(javaClass, classSymbol, parentClassSymbol, classId, javaTypeParameterStack)
        parentClassTypeParameterStackCache.remove(classSymbol)
        parentClassEffectiveVisibilityCache.remove(classSymbol)

        // There's a bit of an ordering restriction here:
        // 1. annotations should be added after the symbol is bound, as annotations can refer to the class itself;
        // 2. type enhancement requires annotations to be already present (and supertypes can refer to type parameters).
        firJavaClass.annotations.addFromJava(session, javaClass, javaTypeParameterStack)
        val enhancement = FirSignatureEnhancement(firJavaClass, session) { emptyList() }
        enhancement.enhanceTypeParameterBounds(firJavaClass.typeParameters)
        val enhancedSuperTypes = buildList {
            val purelyImplementedSupertype = firJavaClass.getPurelyImplementedSupertype()
            val purelyImplementedSupertypeClassId = purelyImplementedSupertype?.classId
            firJavaClass.superTypeRefs.mapNotNullTo(this) { superType ->
                enhancement.enhanceSuperType(superType).takeUnless {
                    purelyImplementedSupertypeClassId != null && it.coneType.classId == purelyImplementedSupertypeClassId
                }
            }
            purelyImplementedSupertype?.let {
                add(buildResolvedTypeRef { type = it })
            }
        }
        firJavaClass.replaceSuperTypeRefs(enhancedSuperTypes)
        firJavaClass.replaceDeprecation(firJavaClass.getDeprecationInfos(session.languageVersionSettings.apiVersion))
        return firJavaClass
    }

    private fun FirJavaClass.getPurelyImplementedSupertype(): ConeKotlinType? {
        val purelyImplementedClassIdFromAnnotation = annotations
            .firstOrNull { it.classId?.asSingleFqName() == JvmAnnotationNames.PURELY_IMPLEMENTS_ANNOTATION }
            ?.let { (it.argumentMapping.mapping.values.firstOrNull() as? FirConstExpression<*>) }
            ?.let { it.value as? String }
            ?.takeIf { it.isNotBlank() && isValidJavaFqName(it) }
            ?.let { ClassId.topLevel(FqName(it)) }
        val purelyImplementedClassId = purelyImplementedClassIdFromAnnotation
            ?: FakePureImplementationsProvider.getPurelyImplementedInterface(symbol.classId)
            ?: return null
        val superTypeSymbol = session.symbolProvider.getClassLikeSymbolByClassId(purelyImplementedClassId) ?: return null
        val superTypeParameterSymbols = superTypeSymbol.typeParameterSymbols ?: return null
        val typeParameters = this.typeParameters
        val supertypeParameterCount = superTypeParameterSymbols.size
        val typeParameterCount = typeParameters.size
        val parametersAsTypeProjections = when {
            typeParameterCount == supertypeParameterCount ->
                typeParameters.map { ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(it.symbol), isNullable = false) }
            typeParameterCount == 1 && supertypeParameterCount > 1 && purelyImplementedClassIdFromAnnotation == null -> {
                val projection = ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(typeParameters.first().symbol), isNullable = false)
                (1..supertypeParameterCount).map { projection }
            }
            else -> return null
        }
        return ConeClassLikeTypeImpl(
            ConeClassLikeLookupTagImpl(purelyImplementedClassId),
            parametersAsTypeProjections.toTypedArray(),
            isNullable = false
        )
    }

    private fun createFirJavaClass(
        javaClass: JavaClass,
        classSymbol: FirRegularClassSymbol,
        parentClassSymbol: FirRegularClassSymbol?,
        classId: ClassId,
        javaTypeParameterStack: JavaTypeParameterStack,
    ): FirJavaClass {
        val valueParametersForAnnotationConstructor = ValueParametersForAnnotationConstructor()
        val classIsAnnotation = javaClass.classKind == ClassKind.ANNOTATION_CLASS
        val moduleData = getModuleDataForClass(javaClass)
        return buildJavaClass {
            source = (javaClass as? JavaElementImpl<*>)?.psi?.toKtPsiSourceElement()
            this.moduleData = moduleData
            symbol = classSymbol
            name = javaClass.name
            val visibility = javaClass.visibility
            this@buildJavaClass.visibility = visibility
            classKind = javaClass.classKind
            modality = if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) Modality.FINAL else javaClass.modality
            this.isTopLevel = classId.outerClassId == null
            isStatic = javaClass.isStatic
            javaPackage = packageCache.getValue(classSymbol.classId.packageFqName)
            this.javaTypeParameterStack = javaTypeParameterStack
            existingNestedClassifierNames += javaClass.innerClassNames
            scopeProvider = JavaScopeProvider

            val selfEffectiveVisibility = visibility.toEffectiveVisibility(parentClassSymbol?.toLookupTag(), forClass = true)
            val parentEffectiveVisibility = parentClassSymbol?.let {
                parentClassEffectiveVisibilityCache[it] ?: it.fir.effectiveVisibility
            } ?: EffectiveVisibility.Public
            val effectiveVisibility = parentEffectiveVisibility.lowerBound(selfEffectiveVisibility, session.typeContext)
            parentClassEffectiveVisibilityCache[classSymbol] = effectiveVisibility

            val classTypeParameters = javaClass.typeParameters.convertTypeParameters(javaTypeParameterStack, classSymbol, moduleData)
            typeParameters += classTypeParameters
            if (!isStatic && parentClassSymbol != null) {
                typeParameters += parentClassSymbol.fir.typeParameters.map {
                    buildOuterClassTypeParameterRef { symbol = it.symbol }
                }
            }
            javaClass.supertypes.mapTo(superTypeRefs) { it.toFirJavaTypeRef(session, javaTypeParameterStack) }
            if (superTypeRefs.isEmpty()) {
                superTypeRefs.add(
                    buildResolvedTypeRef {
                        type = StandardClassIds.Any.constructClassLikeType(emptyArray(), isNullable = false)
                    }
                )
            }

            val dispatchReceiver = classId.defaultType(typeParameters.map { it.symbol })

            status = FirResolvedDeclarationStatusImpl(
                visibility,
                modality!!,
                effectiveVisibility
            ).apply {
                this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
                isCompanion = false
                isData = false
                isInline = false
                isFun = classKind == ClassKind.INTERFACE
            }
            // TODO: may be we can process fields & methods later.
            // However, they should be built up to override resolve stage
            for (javaField in javaClass.fields) {
                declarations += convertJavaFieldToFir(javaField, classId, javaTypeParameterStack, dispatchReceiver, moduleData)
            }

            for (javaMethod in javaClass.methods) {
                if (javaMethod.isObjectMethodInInterface()) continue
                val firJavaMethod = convertJavaMethodToFir(
                    javaMethod,
                    classId,
                    javaTypeParameterStack,
                    dispatchReceiver,
                    moduleData,
                )
                declarations += firJavaMethod

                if (classIsAnnotation) {
                    val parameterForAnnotationConstructor =
                        convertJavaAnnotationMethodToValueParameter(javaMethod, firJavaMethod, moduleData)
                    if (javaMethod.name == VALUE_METHOD_NAME) {
                        valueParametersForAnnotationConstructor.valueParameterForValue = javaMethod to parameterForAnnotationConstructor
                    } else {
                        valueParametersForAnnotationConstructor.valueParameters[javaMethod] = parameterForAnnotationConstructor
                    }
                }
            }
            val javaClassDeclaredConstructors = javaClass.constructors
            val constructorId = CallableId(classId.packageFqName, classId.relativeClassName, classId.shortClassName)

            if (javaClassDeclaredConstructors.isEmpty()
                && javaClass.classKind == ClassKind.CLASS
                && javaClass.hasDefaultConstructor()
            ) {
                declarations += convertJavaConstructorToFir(
                    javaConstructor = null,
                    constructorId,
                    javaClass,
                    ownerClassBuilder = this,
                    classTypeParameters,
                    javaTypeParameterStack,
                    parentClassSymbol,
                    moduleData,
                )
            }
            for (javaConstructor in javaClassDeclaredConstructors) {
                declarations += convertJavaConstructorToFir(
                    javaConstructor,
                    constructorId,
                    javaClass,
                    ownerClassBuilder = this,
                    classTypeParameters,
                    javaTypeParameterStack,
                    parentClassSymbol,
                    moduleData,
                )
            }

            if (classKind == ClassKind.ENUM_CLASS) {
                generateValuesFunction(
                    moduleData,
                    classId.packageFqName,
                    classId.relativeClassName
                )
                generateValueOfFunction(moduleData, classId.packageFqName, classId.relativeClassName)
            }
            if (classIsAnnotation) {
                declarations +=
                    buildConstructorForAnnotationClass(
                        classSource = (javaClass as? JavaElementImpl<*>)?.psi?.toKtPsiSourceElement(KtFakeSourceElementKind.ImplicitConstructor) as? KtFakeSourceElement,
                        constructorId = constructorId,
                        ownerClassBuilder = this,
                        valueParametersForAnnotationConstructor = valueParametersForAnnotationConstructor,
                        moduleData = moduleData,
                    )
            }
        }.apply {
            if (modality == Modality.SEALED) {
                val inheritors = javaClass.permittedTypes.mapNotNull { classifierType ->
                    val classifier = classifierType.classifier as? JavaClass
                    classifier?.let { JavaToKotlinClassMap.mapJavaToKotlin(it.fqName!!) }
                }
                setSealedClassInheritors(inheritors)
            }

            if (classIsAnnotation) {
                // Cannot load these until the symbol is bound because they may be self-referential.
                valueParametersForAnnotationConstructor.forEach { javaMethod, firValueParameter ->
                    javaMethod.annotationParameterDefaultValue?.let { javaDefaultValue ->
                        firValueParameter.defaultValue =
                            javaDefaultValue.toFirExpression(session, javaTypeParameterStack, firValueParameter.returnTypeRef)
                    }
                }
            }
        }
    }

    private fun convertJavaFieldToFir(
        javaField: JavaField,
        classId: ClassId,
        javaTypeParameterStack: JavaTypeParameterStack,
        dispatchReceiver: ConeClassLikeType,
        moduleData: FirModuleData,
    ): FirDeclaration {
        val fieldName = javaField.name
        val fieldId = CallableId(classId.packageFqName, classId.relativeClassName, fieldName)
        val returnType = javaField.type
        return when {
            javaField.isEnumEntry -> buildEnumEntry {
                source = (javaField as? JavaElementImpl<*>)?.psi?.toKtPsiSourceElement()
                this.moduleData = moduleData
                symbol = FirEnumEntrySymbol(fieldId)
                name = fieldName
                status = FirResolvedDeclarationStatusImpl(
                    javaField.visibility,
                    javaField.modality,
                    javaField.visibility.toEffectiveVisibility(dispatchReceiver.lookupTag)
                ).apply {
                    isStatic = javaField.isStatic
                    isExpect = false
                    isActual = false
                    isOverride = false
                }
                returnTypeRef = returnType.toFirJavaTypeRef(session, javaTypeParameterStack)
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                origin = FirDeclarationOrigin.Java
                // TODO: check if this works properly with annotations that take the enum class as an argument
                annotations.addFromJava(session, javaField, javaTypeParameterStack)
            }.apply {
                containingClassForStaticMemberAttr = ConeClassLikeLookupTagImpl(classId)
            }
            else -> buildJavaField {
                source = (javaField as? JavaElementImpl<*>)?.psi?.toKtPsiSourceElement()
                this.moduleData = moduleData
                symbol = FirFieldSymbol(fieldId)
                name = fieldName
                status = FirResolvedDeclarationStatusImpl(
                    javaField.visibility,
                    javaField.modality,
                    javaField.visibility.toEffectiveVisibility(dispatchReceiver.lookupTag)
                ).apply {
                    isStatic = javaField.isStatic
                    isExpect = false
                    isActual = false
                    isOverride = false
                }
                visibility = javaField.visibility
                modality = javaField.modality
                returnTypeRef = returnType.toFirJavaTypeRef(session, javaTypeParameterStack)
                isVar = !javaField.isFinal
                isStatic = javaField.isStatic
                annotationBuilder = { javaField.convertAnnotationsToFir(session, javaTypeParameterStack) }
                initializer = convertJavaInitializerToFir(javaField.initializerValue)

                if (!javaField.isStatic) {
                    dispatchReceiverType = dispatchReceiver
                }
            }.apply {
                if (javaField.isStatic) {
                    containingClassForStaticMemberAttr = ConeClassLikeLookupTagImpl(classId)
                }
            }
        }
    }

    private fun convertJavaInitializerToFir(value: Any?): FirExpression? {
        // NB: null should be converted to null
        return value?.createConstantIfAny(session)
    }

    private fun convertJavaMethodToFir(
        javaMethod: JavaMethod,
        classId: ClassId,
        javaTypeParameterStack: JavaTypeParameterStack,
        dispatchReceiver: ConeClassLikeType,
        moduleData: FirModuleData,
    ): FirJavaMethod {
        val methodName = javaMethod.name
        val methodId = CallableId(classId.packageFqName, classId.relativeClassName, methodName)
        val methodSymbol = FirNamedFunctionSymbol(methodId)
        val returnType = javaMethod.returnType
        return buildJavaMethod {
            this.moduleData = moduleData
            source = (javaMethod as? JavaElementImpl<*>)?.psi?.toKtPsiSourceElement()
            symbol = methodSymbol
            name = methodName
            returnTypeRef = returnType.toFirJavaTypeRef(session, javaTypeParameterStack)
            isStatic = javaMethod.isStatic
            typeParameters += javaMethod.typeParameters.convertTypeParameters(javaTypeParameterStack, methodSymbol, moduleData)
            for ((index, valueParameter) in javaMethod.valueParameters.withIndex()) {
                valueParameters += valueParameter.toFirValueParameter(session, moduleData, index, javaTypeParameterStack)
            }
            annotationBuilder = { javaMethod.convertAnnotationsToFir(session, javaTypeParameterStack) }
            status = FirResolvedDeclarationStatusImpl(
                javaMethod.visibility,
                javaMethod.modality,
                javaMethod.visibility.toEffectiveVisibility(dispatchReceiver.lookupTag)
            ).apply {
                isStatic = javaMethod.isStatic
                isExpect = false
                isActual = false
                isOverride = false
                // Approximation: all Java methods with name that allows to use it in operator form are considered operators
                // We need here more detailed checks (see modifierChecks.kt)
                isOperator = name in ALL_JAVA_OPERATION_NAMES || OperatorNameConventions.COMPONENT_REGEX.matches(name.asString())
                isInfix = false
                isInline = false
                isTailRec = false
                isExternal = false
                isSuspend = false
            }

            if (!javaMethod.isStatic) {
                dispatchReceiverType = dispatchReceiver
            }
        }.apply {
            if (javaMethod.isStatic) {
                containingClassForStaticMemberAttr = ConeClassLikeLookupTagImpl(classId)
            }
        }
    }

    private fun convertJavaAnnotationMethodToValueParameter(
        javaMethod: JavaMethod,
        firJavaMethod: FirJavaMethod,
        moduleData: FirModuleData,
    ): FirJavaValueParameter =
        buildJavaValueParameter {
            source = (javaMethod as? JavaElementImpl<*>)?.psi
                ?.toKtPsiSourceElement(KtFakeSourceElementKind.ImplicitJavaAnnotationConstructor)
            this.moduleData = moduleData
            returnTypeRef = firJavaMethod.returnTypeRef
            name = javaMethod.name
            isVararg = javaMethod.returnType is JavaArrayType && javaMethod.name == VALUE_METHOD_NAME
            annotationBuilder = { emptyList() }
        }

    private fun convertJavaConstructorToFir(
        javaConstructor: JavaConstructor?,
        constructorId: CallableId,
        javaClass: JavaClass,
        ownerClassBuilder: FirJavaClassBuilder,
        classTypeParameters: List<FirTypeParameter>,
        javaTypeParameterStack: JavaTypeParameterStack,
        outerClassSymbol: FirRegularClassSymbol?,
        moduleData: FirModuleData,
    ): FirJavaConstructor {
        val constructorSymbol = FirConstructorSymbol(constructorId)
        return buildJavaConstructor {
            source = (javaConstructor as? JavaElementImpl<*>)?.psi?.toKtPsiSourceElement()
            this.moduleData = moduleData
            symbol = constructorSymbol
            isInner = javaClass.outerClass != null && !javaClass.isStatic
            val isThisInner = this.isInner
            val visibility = javaConstructor?.visibility ?: ownerClassBuilder.visibility
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                visibility.toEffectiveVisibility(ownerClassBuilder.symbol)
            ).apply {
                isExpect = false
                isActual = false
                isOverride = false
                isInner = isThisInner
            }
            this.visibility = visibility
            isPrimary = javaConstructor == null
            returnTypeRef = buildResolvedTypeRef {
                type = ownerClassBuilder.buildSelfTypeRef()
            }
            dispatchReceiverType = if (isThisInner) outerClassSymbol?.defaultType() else null
            typeParameters += classTypeParameters.map { buildConstructedClassTypeParameterRef { symbol = it.symbol } }

            if (javaConstructor != null) {
                this.typeParameters += javaConstructor.typeParameters.convertTypeParameters(javaTypeParameterStack, constructorSymbol, moduleData)
                annotationBuilder = { javaConstructor.convertAnnotationsToFir(session, javaTypeParameterStack) }
                for ((index, valueParameter) in javaConstructor.valueParameters.withIndex()) {
                    valueParameters += valueParameter.toFirValueParameter(session, moduleData, index, javaTypeParameterStack)
                }
            } else {
                annotationBuilder = { emptyList() }
            }
        }.apply {
            containingClassForStaticMemberAttr = ownerClassBuilder.symbol.toLookupTag()
        }
    }

    private fun buildConstructorForAnnotationClass(
        classSource: KtFakeSourceElement?,
        constructorId: CallableId,
        ownerClassBuilder: FirJavaClassBuilder,
        valueParametersForAnnotationConstructor: ValueParametersForAnnotationConstructor,
        moduleData: FirModuleData,
    ): FirJavaConstructor {
        return buildJavaConstructor {
            source = classSource
            this.moduleData = moduleData
            symbol = FirConstructorSymbol(constructorId)
            status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
            returnTypeRef = buildResolvedTypeRef {
                type = ownerClassBuilder.buildSelfTypeRef()
            }
            valueParametersForAnnotationConstructor.forEach { _, firValueParameter -> valueParameters += firValueParameter }
            visibility = Visibilities.Public
            isInner = false
            isPrimary = true
            annotationBuilder = { emptyList() }
        }.apply {
            containingClassForStaticMemberAttr = ownerClassBuilder.symbol.toLookupTag()
        }
    }

    private fun FirJavaClassBuilder.buildSelfTypeRef(): ConeKotlinType = symbol.constructType(
        typeParameters.map {
            ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), isNullable = false)
        }.toTypedArray(),
        isNullable = false,
    )

    private fun FqName.topLevelName() =
        asString().substringBefore(".")
}
