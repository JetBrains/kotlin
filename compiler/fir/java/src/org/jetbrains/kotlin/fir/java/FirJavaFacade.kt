/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildEnumEntry
import org.jetbrains.kotlin.fir.declarations.builder.buildOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.statusTransformerExtensions
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.enhancement.FirSignatureEnhancement
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.name.*
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
    private val statusExtensions = session.extensionService.statusTransformerExtensions

    fun findClass(classId: ClassId, knownContent: ByteArray? = null): JavaClass? =
        classFinder.findClass(JavaClassFinder.Request(classId, knownContent))
            ?.takeIf { it.classId == classId && !it.hasMetadataAnnotation() }

    fun getPackage(fqName: FqName): FqName? =
        packageCache.getValue(fqName)?.fqName

    fun hasTopLevelClassOf(classId: ClassId): Boolean {
        val knownNames = knownClassNamesInPackage.getValue(classId.packageFqName) ?: return true
        return classId.relativeClassName.topLevelName() in knownNames
    }

    fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? = knownClassNamesInPackage.getValue(packageFqName)

    abstract fun getModuleDataForClass(javaClass: JavaClass): FirModuleData

    private fun JavaTypeParameter.toFirTypeParameter(
        javaTypeParameterStack: JavaTypeParameterStack,
        containingDeclarationSymbol: FirBasedSymbol<*>,
        moduleData: FirModuleData,
    ): FirTypeParameter {
        return buildTypeParameter {
            this.moduleData = moduleData
            origin = javaOrigin(isFromSource)
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            name = this@toFirTypeParameter.name
            symbol = FirTypeParameterSymbol()
            variance = INVARIANT
            isReified = false
            javaTypeParameterStack.addParameter(this@toFirTypeParameter, symbol)
            this.containingDeclarationSymbol = containingDeclarationSymbol
            for (upperBound in this@toFirTypeParameter.upperBounds) {
                bounds += upperBound.toFirJavaTypeRef(session, javaTypeParameterStack)
            }
            if (bounds.isEmpty()) {
                bounds += buildResolvedTypeRef {
                    type = ConeFlexibleType(builtinTypes.anyType.type, builtinTypes.nullableAnyType.type)
                }
            }
        }.apply {
            // TODO: should be lazy (in case annotations refer to the containing class)
            setAnnotationsFromJava(session, this@toFirTypeParameter, javaTypeParameterStack)
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

        // This is where the problems begin. We need to enhance nullability of super types and type parameter bounds,
        // for which we need the annotations of this class as they may specify default nullability.
        // However, all three - annotations, type parameter bounds, and supertypes - can refer to other classes,
        // which will cause the type parameter bounds and supertypes of *those* classes to get enhanced first,
        // but they may refer back to this class again - which, thanks to the magic of symbol resolver caches,
        // will be observed in a state where we've not done the enhancement yet. For those cases, we must publish
        // at least unenhanced resolved types, or else FIR may crash upon encountering a FirJavaTypeRef where FirResolvedTypeRef
        // is expected.
        // TODO: some (all?) of those loops can be avoided, e.g. we don't actually need to resolve class arguments of annotations
        //   to determine whether they set default nullability - but without laziness, breaking those loops is somewhat hard,
        //   as we have a nested ordering here.

        val enhancement = FirSignatureEnhancement(firJavaClass, session) { emptyList() }
        val initialBounds = enhancement.performFirstRoundOfBoundsResolution(firJavaClass.typeParameters)

        // 1. (will happen lazily in FirJavaClass.annotations) Resolve annotations
        // 2. Enhance type parameter bounds - may refer to each other, take default nullability from annotations
        // 3. (will happen lazily in FirJavaClass.superTypeRefs) Enhance super types - may refer to type parameter bounds, take default nullability from annotations

        enhancement.enhanceTypeParameterBoundsAfterFirstRound(firJavaClass.typeParameters, initialBounds)

        updateStatuses(firJavaClass, parentClassSymbol)

        return firJavaClass
    }

    private fun updateStatuses(firJavaClass: FirJavaClass, parentClassSymbol: FirRegularClassSymbol?) {
        if (statusExtensions.isEmpty()) return
        val classSymbol = firJavaClass.symbol
        val visitor = object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {}

            override fun visitRegularClass(regularClass: FirRegularClass) {
                regularClass.applyExtensionTransformers {
                    transformStatus(it, regularClass, parentClassSymbol, isLocal = false)
                }
                regularClass.acceptChildren(this)
            }

            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
                simpleFunction.applyExtensionTransformers {
                    transformStatus(it, simpleFunction, classSymbol, isLocal = false)
                }
            }

            override fun visitField(field: FirField) {
                field.applyExtensionTransformers {
                    transformStatus(it, field, classSymbol, isLocal = false)
                }
            }

            override fun visitConstructor(constructor: FirConstructor) {
                constructor.applyExtensionTransformers {
                    transformStatus(it, constructor, classSymbol, isLocal = false)
                }
            }
        }
        firJavaClass.accept(visitor)
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
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            javaAnnotations += javaClass.annotations
            source = javaClass.toSourceElement()
            this.moduleData = moduleData
            symbol = classSymbol
            name = javaClass.name
            isFromSource = javaClass.isFromSource
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
                    javaClass,
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
                generateEntriesGetter(moduleData, classId.packageFqName, classId.relativeClassName)
            }
            if (classIsAnnotation) {
                declarations +=
                    buildConstructorForAnnotationClass(
                        javaClass,
                        constructorId = constructorId,
                        ownerClassBuilder = this,
                        valueParametersForAnnotationConstructor = valueParametersForAnnotationConstructor,
                        moduleData = moduleData,
                    )
            }
            if (javaClass.isRecord) {
                createDeclarationsForJavaRecord(
                    javaClass,
                    classId,
                    moduleData,
                    javaTypeParameterStack,
                    dispatchReceiver,
                    classTypeParameters,
                    declarations
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

            if (javaClass.isRecord) {
                this.isJavaRecord = true
            }
        }
    }

    private fun createDeclarationsForJavaRecord(
        javaClass: JavaClass,
        classId: ClassId,
        moduleData: FirModuleData,
        javaTypeParameterStack: JavaTypeParameterStack,
        classType: ConeClassLikeType,
        classTypeParameters: List<FirTypeParameter>,
        destination: MutableList<FirDeclaration>
    ) {
        val functionsByName = destination.filterIsInstance<FirJavaMethod>().groupBy { it.name }

        for (recordComponent in javaClass.recordComponents) {
            val name = recordComponent.name
            if (functionsByName[name].orEmpty().any { it.valueParameters.isEmpty() }) continue

            val componentId = CallableId(classId, name)
            destination += buildJavaMethod {
                this.moduleData = moduleData
                source = recordComponent.toSourceElement(KtFakeSourceElementKind.JavaRecordComponentFunction)
                symbol = FirNamedFunctionSymbol(componentId)
                this.name = name
                isFromSource = recordComponent.isFromSource
                returnTypeRef = recordComponent.type.toFirJavaTypeRef(session, javaTypeParameterStack)
                annotationBuilder = { emptyList() }
                status = FirResolvedDeclarationStatusImpl(
                    Visibilities.Public,
                    Modality.FINAL,
                    EffectiveVisibility.Public
                )
                dispatchReceiverType = classType
            }.apply {
                isJavaRecordComponent = true
            }
        }

        destination += buildJavaConstructor {
            source = javaClass.toSourceElement(KtFakeSourceElementKind.ImplicitJavaRecordConstructor)
            this.moduleData = moduleData
            isFromSource = javaClass.isFromSource

            val constructorId = CallableId(classId, classId.shortClassName)
            symbol = FirConstructorSymbol(constructorId)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            visibility = Visibilities.Public
            isPrimary = true
            returnTypeRef = classType.toFirResolvedTypeRef()
            dispatchReceiverType = null
            typeParameters += classTypeParameters.toRefs()
            annotationBuilder = { emptyList() }

            javaClass.recordComponents.mapTo(valueParameters) { component ->
                buildJavaValueParameter {
                    containingFunctionSymbol = this@buildJavaConstructor.symbol
                    source = component.toSourceElement(KtFakeSourceElementKind.ImplicitRecordConstructorParameter)
                    this.moduleData = moduleData
                    isFromSource = component.isFromSource
                    returnTypeRef = component.type.toFirJavaTypeRef(session, javaTypeParameterStack)
                    name = component.name
                    isVararg = component.isVararg
                    annotationBuilder = { emptyList() }
                }
            }
        }.apply {
            containingClassForStaticMemberAttr = classType.lookupTag
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
                source = javaField.toSourceElement()
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
                origin = javaOrigin(javaField.isFromSource)
            }.apply {
                containingClassForStaticMemberAttr = classId.toLookupTag()
                // TODO: check if this works properly with annotations that take the enum class as an argument
                setAnnotationsFromJava(session, javaField, javaTypeParameterStack)
            }
            else -> buildJavaField {
                source = javaField.toSourceElement()
                this.moduleData = moduleData
                symbol = FirFieldSymbol(fieldId)
                name = fieldName
                isFromSource = javaField.isFromSource
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

                lazyInitializer = lazy {
                    // NB: null should be converted to null
                    javaField.initializerValue?.createConstantIfAny(session)
                }

                if (!javaField.isStatic) {
                    dispatchReceiverType = dispatchReceiver
                }
            }.apply {
                if (javaField.isStatic) {
                    containingClassForStaticMemberAttr = classId.toLookupTag()
                }
            }
        }
    }

    private fun convertJavaMethodToFir(
        containingClass: JavaClass,
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
            source = javaMethod.toSourceElement()
            symbol = methodSymbol
            name = methodName
            isFromSource = javaMethod.isFromSource
            returnTypeRef = returnType.toFirJavaTypeRef(session, javaTypeParameterStack)
            isStatic = javaMethod.isStatic
            typeParameters += javaMethod.typeParameters.convertTypeParameters(javaTypeParameterStack, methodSymbol, moduleData)
            for ((index, valueParameter) in javaMethod.valueParameters.withIndex()) {
                valueParameters += valueParameter.toFirValueParameter(session, methodSymbol, moduleData, index, javaTypeParameterStack)
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
                containingClassForStaticMemberAttr = classId.toLookupTag()
            }
            if (containingClass.isRecord && valueParameters.isEmpty() && containingClass.recordComponents.any { it.name == methodName }) {
                isJavaRecordComponent = true
            }
        }
    }

    private fun convertJavaAnnotationMethodToValueParameter(
        javaMethod: JavaMethod,
        firJavaMethod: FirJavaMethod,
        moduleData: FirModuleData,
    ): FirJavaValueParameter =
        buildJavaValueParameter {
            source = javaMethod.toSourceElement(KtFakeSourceElementKind.ImplicitJavaAnnotationConstructor)
            this.moduleData = moduleData
            isFromSource = javaMethod.isFromSource
            returnTypeRef = firJavaMethod.returnTypeRef
            containingFunctionSymbol = firJavaMethod.symbol
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
            source = javaConstructor?.toSourceElement() ?: javaClass.toSourceElement(KtFakeSourceElementKind.ImplicitConstructor)
            this.moduleData = moduleData
            isFromSource = javaClass.isFromSource
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
            typeParameters += classTypeParameters.toRefs()

            if (javaConstructor != null) {
                this.typeParameters += javaConstructor.typeParameters.convertTypeParameters(javaTypeParameterStack, constructorSymbol, moduleData)
                annotationBuilder = { javaConstructor.convertAnnotationsToFir(session, javaTypeParameterStack) }
                for ((index, valueParameter) in javaConstructor.valueParameters.withIndex()) {
                    valueParameters += valueParameter.toFirValueParameter(session, constructorSymbol, moduleData, index, javaTypeParameterStack)
                }
            } else {
                annotationBuilder = { emptyList() }
            }
        }.apply {
            containingClassForStaticMemberAttr = ownerClassBuilder.symbol.toLookupTag()
        }
    }

    private fun buildConstructorForAnnotationClass(
        javaClass: JavaClass,
        constructorId: CallableId,
        ownerClassBuilder: FirJavaClassBuilder,
        valueParametersForAnnotationConstructor: ValueParametersForAnnotationConstructor,
        moduleData: FirModuleData,
    ): FirJavaConstructor {
        return buildJavaConstructor {
            source = javaClass.toSourceElement(KtFakeSourceElementKind.ImplicitConstructor)
            this.moduleData = moduleData
            isFromSource = javaClass.isFromSource
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

    private fun JavaElement.toSourceElement(sourceElementKind: KtSourceElementKind = KtRealSourceElementKind): KtSourceElement? {
        return (this as? JavaElementImpl<*>)?.psi?.toKtPsiSourceElement(sourceElementKind)
    }

    private fun List<FirTypeParameter>.toRefs(): List<FirTypeParameterRef> {
        return this.map { buildConstructedClassTypeParameterRef { symbol = it.symbol } }
    }

    private inline fun FirMemberDeclaration.applyExtensionTransformers(
        operation: FirStatusTransformerExtension.(FirDeclarationStatus) -> FirDeclarationStatus
    ) {
        val declaration = this
        val oldStatus = declaration.status as FirResolvedDeclarationStatusImpl
        val newStatus = statusExtensions.fold(status) { acc, it ->
            if (it.needTransformStatus(declaration)) {
                it.operation(acc)
            } else {
                acc
            }
        } as FirDeclarationStatusImpl
        if (newStatus === oldStatus) return
        val resolvedStatus = newStatus.resolved(
            newStatus.visibility,
            newStatus.modality ?: oldStatus.modality,
            oldStatus.effectiveVisibility
        )
        replaceStatus(resolvedStatus)
    }
}
