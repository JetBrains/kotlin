/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.psi.PsiMethod
import com.intellij.psi.util.JavaPsiRecordUtil
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
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaDeclarationList
import org.jetbrains.kotlin.fir.java.enhancement.FirLazyJavaAnnotationList
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirJavaFacadeForSource(
    session: FirSession,
    private val sourceModuleData: FirModuleData,
    classFinder: JavaClassFinder,
) : FirJavaFacade(session, classFinder) {
    override fun getModuleDataForClass(javaClass: JavaClass): FirModuleData {
        return sourceModuleData
    }
}

@ThreadSafeMutableState
abstract class FirJavaFacade(session: FirSession, private val classFinder: JavaClassFinder) {
    companion object {
        val VALUE_METHOD_NAME: Name = Name.identifier("value")
        private const val PACKAGE_INFO_CLASS_NAME = "package-info"
    }

    private val packageCache = session.firCachesFactory.createCache { fqName: FqName ->
        val knownClassNames: Set<String>? = knownClassNamesInPackage(fqName)
        classFinder.findPackage(
            fqName,
            mayHaveAnnotations = if (knownClassNames != null) PACKAGE_INFO_CLASS_NAME in knownClassNames else true
        )
    }

    private val knownClassNamesInPackage = session.firCachesFactory.createCache(classFinder::knownClassNamesInPackage)

    fun findClass(classId: ClassId, knownContent: ByteArray? = null): JavaClass? =
        classFinder.findClass(JavaClassFinder.Request(classId, knownContent))?.takeUnless(JavaClass::hasMetadataAnnotation)

    fun hasPackage(fqName: FqName): Boolean =
        packageCache.getValue(fqName) != null

    fun hasTopLevelClassOf(classId: ClassId): Boolean {
        val knownNames = knownClassNamesInPackage(classId.packageFqName) ?: return true
        return classId.relativeClassName.topLevelName() in knownNames
    }

    fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? {
        // Avoid filling the cache with `null`s and accessing the cache if `knownClassNamesInPackage` cannot be calculated anyway.
        if (!classFinder.canComputeKnownClassNamesInPackage()) return null
        return knownClassNamesInPackage.getValue(packageFqName)
    }

    abstract fun getModuleDataForClass(javaClass: JavaClass): FirModuleData

    /**
     * Guarantees that this conversion is free from other classes access.
     *
     * All required accesses will be performed on demand lazily after publication.
     */
    fun convertJavaClassToFir(
        classSymbol: FirRegularClassSymbol,
        parentClassSymbol: FirRegularClassSymbol?,
        javaClass: JavaClass,
    ): FirJavaClass {
        val classId = classSymbol.classId
        val javaTypeParameterStack = MutableJavaTypeParameterStack()

        if (parentClassSymbol != null) {
            val parentStack = (parentClassSymbol.fir as FirJavaClass).classJavaTypeParameterStack
            javaTypeParameterStack.addStack(parentStack)
        }

        val firJavaClass = createFirJavaClass(javaClass, classSymbol, parentClassSymbol, classId, javaTypeParameterStack)

        /**
         * This is where the problems begin. We need to enhance nullability of super types and type parameter bounds,
         * for which we need the annotations of this class as they may specify default nullability.
         * However, all three - annotations, type parameter bounds, and supertypes - can refer to other classes,
         * which will cause the type parameter bounds and supertypes of *those* classes to get enhanced first,
         * but they may refer back to this class again - which, thanks to the magic of symbol resolver caches,
         * will be observed in a state where we've not done the enhancement yet. For those cases, we must publish
         * at least unenhanced resolved types,
         * or else FIR may crash upon encountering a [org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef]
         * where [FirResolvedTypeRef] is expected.
         *
         * 1. (will happen lazily in [FirJavaClass.annotations]]) Resolve annotations
         * 2. (will happen lazily in [FirJavaClass.typeParameters]) Enhance type parameter bounds in [FirJavaTypeParameter] - may refer to each other, take default nullability from annotations
         * 3. (will happen lazily in [FirJavaClass.superTypeRefs]) Enhance super types - may refer to type parameter bounds, take default nullability from annotations
         */
        return firJavaClass
    }

    private fun createFirJavaClass(
        javaClass: JavaClass,
        classSymbol: FirRegularClassSymbol,
        parentClassSymbol: FirRegularClassSymbol?,
        classId: ClassId,
        classJavaTypeParameterStack: MutableJavaTypeParameterStack,
    ): FirJavaClass {
        val moduleData = getModuleDataForClass(javaClass)
        val session = moduleData.session
        return buildJavaClass {
            this.javaClass = javaClass
            containingClassSymbol = parentClassSymbol
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            annotationList = FirLazyJavaAnnotationList(javaClass, moduleData)
            source = javaClass.toSourceElement()
            this.moduleData = moduleData
            symbol = classSymbol
            name = javaClass.name
            isFromSource = javaClass.isFromSource
            val visibility = javaClass.visibility
            this@buildJavaClass.visibility = visibility
            classKind = javaClass.classKind
            modality = javaClass.modality
            this.isTopLevel = !classId.isNestedClass
            isStatic = javaClass.isStatic
            javaPackage = packageCache.getValue(classSymbol.classId.packageFqName)
            this.javaTypeParameterStack = classJavaTypeParameterStack
            existingNestedClassifierNames += javaClass.innerClassNames
            scopeProvider = JavaScopeProvider

            val selfEffectiveVisibility = visibility.toEffectiveVisibility(parentClassSymbol?.toLookupTag(), forClass = true)
            val parentEffectiveVisibility = parentClassSymbol?.let {
                // `originalStatus` can be used here as in the current implementation, status compiler plugins
                // cannot change effective visibility.
                (it.fir as FirJavaClass).originalStatus.effectiveVisibility
            } ?: EffectiveVisibility.Public

            val effectiveVisibility = parentEffectiveVisibility.lowerBound(selfEffectiveVisibility, session.typeContext)
            javaClass.typeParameters.mapTo(typeParameters) { javaTypeParameter ->
                javaTypeParameter.toFirTypeParameter(classSymbol, moduleData).also { firTypeParameter ->
                    classJavaTypeParameterStack.addParameter(javaTypeParameter, firTypeParameter.symbol)
                }
            }

            if (!isStatic && parentClassSymbol != null) {
                typeParameters += (parentClassSymbol.fir as FirJavaClass).nonEnhancedTypeParameters.map {
                    buildOuterClassTypeParameterRef { symbol = it.symbol }
                }
            }

            status = FirResolvedDeclarationStatusImpl(
                visibility,
                modality!!,
                effectiveVisibility
            ).apply {
                this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
                isFun = classKind == ClassKind.INTERFACE
            }

            declarationList = FirLazyJavaDeclarationList(javaClass, classSymbol, javaPackage)
        }.apply {
            if (originalStatus.modality == Modality.SEALED) {
                setSealedClassInheritors {
                    javaClass.permittedTypes.mapNotNullTo(mutableListOf()) { classifierType ->
                        val classifier = classifierType.classifier as? JavaClass
                        classifier?.let { JavaToKotlinClassMap.mapJavaToKotlin(it.fqName!!) ?: it.classId }
                    }
                }
            }

            if (javaClass.isRecord) {
                this.isJavaRecord = true
            }

            if (javaClass is VirtualFileBoundJavaClass) {
                javaClass.virtualFile?.let {
                    sourceElement = VirtualFileBasedSourceElement(it)
                }
            }
        }
    }
}

/** @see FirJavaDeclarationList */
private class FirLazyJavaDeclarationList(javaClass: JavaClass, classSymbol: FirRegularClassSymbol, javaPackage: JavaPackage?) : FirJavaDeclarationList {
    /**
     * [LazyThreadSafetyMode.PUBLICATION] is used here to avoid any potential problems with deadlocks
     * as we cannot control how Java resolution will access [declarations].
     */
    override val declarations: List<FirDeclaration> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val declarations = mutableListOf<FirDeclaration>()
        val firJavaClass = classSymbol.fir as FirJavaClass
        val parentClassSymbol = firJavaClass.containingClassSymbol as? FirRegularClassSymbol
        val javaTypeParameterStack = firJavaClass.classJavaTypeParameterStack
        val moduleData = firJavaClass.moduleData
        val session = moduleData.session
        val classId = classSymbol.classId
        val classTypeParameters = firJavaClass.typeParameters.filterIsInstance<FirTypeParameter>()
        val classKind = firJavaClass.classKind
        val classStatus = firJavaClass.originalStatus
        val classResolvePhase = firJavaClass.resolvePhase
        val classSource = firJavaClass.source

        val valueParametersForAnnotationConstructor = ValueParametersForAnnotationConstructor()
        val classIsAnnotation = classKind == ClassKind.ANNOTATION_CLASS
        val dispatchReceiver = firJavaClass.defaultType()

        for (javaField in javaClass.fields) {
            declarations += convertJavaFieldToFir(
                javaField,
                classId,
                javaTypeParameterStack,
                dispatchReceiver,
                moduleData,
                classSymbol,
            )
        }

        for (javaMethod in javaClass.methods) {
            if (javaMethod.isObjectMethodInInterface()) continue
            val firJavaMethod = convertJavaMethodToFir(
                javaClass,
                javaMethod,
                classId,
                dispatchReceiver,
                moduleData,
                classSymbol,
                javaPackage
            )

            declarations += firJavaMethod

            if (classIsAnnotation) {
                val parameterForAnnotationConstructor = convertJavaAnnotationMethodToValueParameter(javaMethod, firJavaMethod, moduleData)
                if (javaMethod.name == FirJavaFacade.VALUE_METHOD_NAME) {
                    valueParametersForAnnotationConstructor.valueParameterForValue = javaMethod to parameterForAnnotationConstructor
                } else {
                    valueParametersForAnnotationConstructor.valueParameters[javaMethod] = parameterForAnnotationConstructor
                }
            }
        }

        val javaClassDeclaredConstructors = javaClass.constructors
        val constructorId = CallableId(classId.packageFqName, classId.relativeClassName, classId.shortClassName)

        if (javaClassDeclaredConstructors.isEmpty()
            && classKind == ClassKind.CLASS
            && !javaClass.isRecord
            && javaClass.hasDefaultConstructor()
        ) {
            declarations += convertJavaConstructorToFir(
                javaConstructor = null,
                constructorId,
                javaClass,
                classSymbol,
                classTypeParameters,
                parentClassSymbol,
                moduleData,
                javaPackage
            )
        }

        for (javaConstructor in javaClassDeclaredConstructors) {
            declarations += convertJavaConstructorToFir(
                javaConstructor,
                constructorId,
                javaClass,
                classSymbol,
                classTypeParameters,
                parentClassSymbol,
                moduleData,
                javaPackage,
            )
        }

        if (classKind == ClassKind.ENUM_CLASS) {
            val mappedJavaEnumFunctionsOrigin = when {
                firJavaClass.origin.fromSource -> FirDeclarationOrigin.Java.Source
                else -> FirDeclarationOrigin.Java.Library
            }

            declarations += generateValuesFunction(
                classSymbol,
                classSource,
                classStatus,
                classResolvePhase,
                moduleData,
                classId.packageFqName,
                classId.relativeClassName,
                origin = mappedJavaEnumFunctionsOrigin,
            )

            declarations += generateValueOfFunction(
                classSymbol,
                classSource,
                classStatus,
                classResolvePhase,
                moduleData,
                classId.packageFqName,
                classId.relativeClassName,
                origin = mappedJavaEnumFunctionsOrigin,
            )

            val enumEntriesOrigin = when {
                firJavaClass.origin.fromSource -> FirDeclarationOrigin.Source
                else -> FirDeclarationOrigin.Library
            }

            declarations += generateEntriesGetter(
                classSymbol,
                classSource,
                classStatus,
                classResolvePhase,
                moduleData,
                classId.packageFqName,
                classId.relativeClassName,
                origin = enumEntriesOrigin,
            )
        }

        if (classIsAnnotation) {
            declarations += buildConstructorForAnnotationClass(
                javaClass,
                constructorId = constructorId,
                classSymbol = classSymbol,
                valueParametersForAnnotationConstructor = valueParametersForAnnotationConstructor,
                moduleData = moduleData,
            )
        }

        // There is no need to generated synthetic declarations for java record from binary dependencies
        //   because they are actually present in .class files
        if (javaClass.isRecord && javaClass.isFromSource) {
            createDeclarationsForJavaRecord(
                javaClass,
                classId,
                moduleData,
                dispatchReceiver,
                classTypeParameters,
                declarations,
                classSymbol,
            )
        }

        if (classIsAnnotation) {
            valueParametersForAnnotationConstructor.forEach { javaMethod, firValueParameter ->
                javaMethod.annotationParameterDefaultValue?.let { javaDefaultValue ->
                    firValueParameter.lazyDefaultValue = lazy {
                        javaDefaultValue.toFirExpression(
                            session,
                            (classSymbol.fir as FirJavaClass).classJavaTypeParameterStack,
                            firValueParameter.returnTypeRef,
                            firValueParameter.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
                        )
                    }
                }
            }
        }

        declarations.ifEmpty { emptyList() }
    }
}

private fun JavaTypeParameter.toFirTypeParameter(
    containingDeclarationSymbol: FirBasedSymbol<*>,
    moduleData: FirModuleData,
): FirTypeParameter = buildJavaTypeParameter {
    javaTypeParameter = this@toFirTypeParameter
    this.moduleData = moduleData
    origin = javaOrigin(isFromSource)
    name = this@toFirTypeParameter.name
    symbol = FirTypeParameterSymbol()
    this.source = this@toFirTypeParameter.toSourceElement()
    this.containingDeclarationSymbol = containingDeclarationSymbol
    annotationList = FirLazyJavaAnnotationList(this@toFirTypeParameter, moduleData)
}

private fun createDeclarationsForJavaRecord(
    javaClass: JavaClass,
    classId: ClassId,
    moduleData: FirModuleData,
    classType: ConeClassLikeType,
    classTypeParameters: List<FirTypeParameter>,
    destination: MutableList<FirDeclaration>,
    containingClassSymbol: FirRegularClassSymbol,
) {
    val session = moduleData.session
    val functionsByName = destination.filterIsInstance<FirJavaMethod>().groupBy { it.name }

    for (recordComponent in javaClass.recordComponents) {
        val name = recordComponent.name
        if (functionsByName[name].orEmpty().any { it.valueParameters.isEmpty() }) continue

        val componentId = CallableId(classId, name)
        destination += buildJavaMethod {
            this.containingClassSymbol = containingClassSymbol
            this.moduleData = moduleData
            source = recordComponent.toSourceElement(KtFakeSourceElementKind.JavaRecordComponentFunction)
            symbol = FirNamedFunctionSymbol(componentId)
            this.name = name
            isFromSource = recordComponent.isFromSource
            returnTypeRef = recordComponent.type.toFirJavaTypeRef(session, source)
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

    /**
     * It is possible that JavaClass already has a synthetic primary constructor ([LightRecordCanonicalConstructor]) or a
     * canonical constructor ([JavaPsiRecordUtil.isCanonicalConstructor]).
     * Such behavior depends on a platform version and psi providers
     * (e.g., in IntelliJ plugin Java class can have additional declarations)
     */
    if (destination.none { it is FirJavaConstructor && it.isPrimary }) {
        destination += buildJavaConstructor {
            this.containingClassSymbol = containingClassSymbol
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
            isPrimary = true
            returnTypeRef = classType.toFirResolvedTypeRef()
            dispatchReceiverType = null
            typeParameters += classTypeParameters.toRefs()

            javaClass.recordComponents.mapTo(valueParameters) { component ->
                buildJavaValueParameter {
                    containingDeclarationSymbol = this@buildJavaConstructor.symbol
                    source = component.toSourceElement(KtFakeSourceElementKind.ImplicitRecordConstructorParameter)
                    this.moduleData = moduleData
                    isFromSource = component.isFromSource
                    returnTypeRef = component.type.toFirJavaTypeRef(session, source)
                    name = component.name
                    isVararg = component.isVararg
                }
            }
        }.apply {
            containingClassForStaticMemberAttr = classType.lookupTag
        }
    }
}

private fun convertJavaFieldToFir(
    javaField: JavaField,
    classId: ClassId,
    javaTypeParameterStack: MutableJavaTypeParameterStack,
    dispatchReceiver: ConeClassLikeType,
    moduleData: FirModuleData,
    containingClassSymbol: FirRegularClassSymbol,
): FirDeclaration {
    val session = moduleData.session
    val fieldName = javaField.name
    val fieldId = CallableId(classId.packageFqName, classId.relativeClassName, fieldName)
    val returnType = javaField.type
    val fakeSource = javaField.toSourceElement()?.fakeElement(KtFakeSourceElementKind.Enhancement)
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
            }
            returnTypeRef = returnType.toFirJavaTypeRef(session, fakeSource)
                .resolveIfJavaType(session, javaTypeParameterStack, fakeSource, mode = FirJavaTypeConversionMode.ANNOTATION_MEMBER)
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            origin = javaOrigin(javaField.isFromSource)
        }.apply {
            containingClassForStaticMemberAttr = classId.toLookupTag()
            // TODO: check if this works properly with annotations that take the enum class as an argument
            setAnnotationsFromJava(session, fakeSource, javaField)
        }
        else -> buildJavaField {
            this.containingClassSymbol = containingClassSymbol
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
            }
            returnTypeRef = returnType.toFirJavaTypeRef(session, fakeSource)
            isVar = !javaField.isFinal
            annotationList = FirLazyJavaAnnotationList(javaField, moduleData)

            lazyInitializer = lazy {
                // NB: null should be converted to null
                javaField.initializerValue?.createConstantIfAny(session)
            }

            lazyHasConstantInitializer = lazy {
                javaField.hasConstantNotNullInitializer
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
    dispatchReceiver: ConeClassLikeType,
    moduleData: FirModuleData,
    containingClassSymbol: FirRegularClassSymbol,
    javaPackage: JavaPackage?,
): FirJavaMethod {
    val session = moduleData.session
    val methodName = javaMethod.name
    val methodId = CallableId(classId.packageFqName, classId.relativeClassName, methodName)
    val methodSymbol = FirNamedFunctionSymbol(methodId)
    val returnType = javaMethod.returnType
    val methodStatus = FirResolvedDeclarationStatusImpl(
        javaMethod.visibility,
        javaMethod.modality,
        javaMethod.visibility.toEffectiveVisibility(dispatchReceiver.lookupTag)
    ).apply {
        isStatic = javaMethod.isStatic
        hasStableParameterNames = false
    }

    return buildJavaMethod {
        this.containingClassSymbol = containingClassSymbol
        this.moduleData = moduleData
        source = javaMethod.toSourceElement()
        symbol = methodSymbol
        name = methodName
        isFromSource = javaMethod.isFromSource
        val fakeSource = source?.fakeElement(KtFakeSourceElementKind.Enhancement)
        returnTypeRef = returnType.toFirJavaTypeRef(session, fakeSource)
        isStatic = javaMethod.isStatic
        javaMethod.typeParameters.mapTo(typeParameters) { it.toFirTypeParameter(methodSymbol, moduleData) }
        for ((index, valueParameter) in javaMethod.valueParameters.withIndex()) {
            valueParameters += valueParameter.toFirValueParameter(session, methodSymbol, moduleData, index)
        }

        annotationList = FirLazyJavaAnnotationList(javaMethod, moduleData)

        status = methodStatus

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
        // Can be called only after .build() because we need to access fir.resolvedAnnotationsWithClassIds
        methodStatus.hasMustUseReturnValue = session.mustUseReturnValueStatusComponent.computeMustUseReturnValueForJavaCallable(
            session,
            methodSymbol,
            containingClassSymbol,
            javaPackage?.annotations?.mapNotNull { it.classId }
        )
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
        containingDeclarationSymbol = firJavaMethod.symbol
        name = javaMethod.name
        isVararg = javaMethod.returnType is JavaArrayType && javaMethod.name == FirJavaFacade.VALUE_METHOD_NAME
    }

private fun convertJavaConstructorToFir(
    javaConstructor: JavaConstructor?,
    constructorId: CallableId,
    javaClass: JavaClass,
    classSymbol: FirRegularClassSymbol,
    classTypeParameters: List<FirTypeParameter>,
    outerClassSymbol: FirRegularClassSymbol?,
    moduleData: FirModuleData,
    javaPackage: JavaPackage?,
): FirJavaConstructor {
    val session = moduleData.session
    val constructorSymbol = FirConstructorSymbol(constructorId)
    val javaFirClass = classSymbol.fir as FirJavaClass
    val visibility = javaConstructor?.visibility ?: javaFirClass.originalStatus.visibility
    val classIsInner = javaClass.outerClass != null && !javaClass.isStatic
    val methodStatus = FirResolvedDeclarationStatusImpl(
        visibility,
        Modality.FINAL,
        visibility.toEffectiveVisibility(classSymbol)
    ).apply {
        isInner = classIsInner
        hasStableParameterNames = false
    }

    return buildJavaConstructor {
        containingClassSymbol = classSymbol
        source = javaConstructor?.toSourceElement() ?: javaClass.toSourceElement(KtFakeSourceElementKind.ImplicitConstructor)
        this.moduleData = moduleData
        isFromSource = javaClass.isFromSource
        symbol = constructorSymbol
        isInner = classIsInner
        status = methodStatus
        // TODO get rid of dependency on PSI KT-63046
        isPrimary = javaConstructor == null || source?.psi.let { it is PsiMethod && JavaPsiRecordUtil.isCanonicalConstructor(it) }
        returnTypeRef = buildResolvedTypeRef {
            coneType = classSymbol.defaultType()
        }
        dispatchReceiverType = if (classIsInner)
            outerClassSymbol?.fir?.let { outerFirJavaClass ->
                outerFirJavaClass as FirJavaClass

                // to avoid type parameter enhancement
                outerClassSymbol.classId.defaultType(outerFirJavaClass.nonEnhancedTypeParameters.map { it.symbol })
            }
        else
            null

        typeParameters += classTypeParameters.toRefs()

        if (javaConstructor != null) {
            javaConstructor.typeParameters.mapTo(typeParameters) { it.toFirTypeParameter(constructorSymbol, moduleData) }

            annotationList = FirLazyJavaAnnotationList(javaConstructor, moduleData)
            for ((index, valueParameter) in javaConstructor.valueParameters.withIndex()) {
                valueParameters += valueParameter.toFirValueParameter(session, constructorSymbol, moduleData, index)
            }
        }
    }.apply {
        containingClassForStaticMemberAttr = classSymbol.toLookupTag()
        // Can be called only after .build() because we need to access fir.resolvedAnnotationsWithClassIds
        methodStatus.hasMustUseReturnValue = session.mustUseReturnValueStatusComponent.computeMustUseReturnValueForJavaCallable(
            session,
            constructorSymbol,
            classSymbol,
            javaPackage?.annotations?.mapNotNull { it.classId }
        )
    }
}

private fun buildConstructorForAnnotationClass(
    javaClass: JavaClass,
    constructorId: CallableId,
    classSymbol: FirRegularClassSymbol,
    valueParametersForAnnotationConstructor: ValueParametersForAnnotationConstructor,
    moduleData: FirModuleData,
): FirJavaConstructor {
    return buildJavaConstructor {
        containingClassSymbol = classSymbol
        source = javaClass.toSourceElement(KtFakeSourceElementKind.ImplicitConstructor)
        this.moduleData = moduleData
        isFromSource = javaClass.isFromSource
        symbol = FirConstructorSymbol(constructorId)
        status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public)
        returnTypeRef = buildResolvedTypeRef {
            coneType = classSymbol.defaultType()
        }
        valueParametersForAnnotationConstructor.forEach { _, firValueParameter -> valueParameters += firValueParameter }
        isInner = false
        isPrimary = true
    }.apply {
        containingClassForStaticMemberAttr = classSymbol.toLookupTag()
    }
}

private fun FqName.topLevelName() = asString().substringBefore(".")

internal fun JavaElement.toSourceElement(sourceElementKind: KtSourceElementKind = KtRealSourceElementKind): KtSourceElement? {
    return (this as? JavaElementImpl<*>)?.psi?.toKtPsiSourceElement(sourceElementKind)
}

private fun List<FirTypeParameter>.toRefs(): List<FirTypeParameterRef> {
    return this.map { buildConstructedClassTypeParameterRef { symbol = it.symbol } }
}

private class ValueParametersForAnnotationConstructor {
    val valueParameters: MutableMap<JavaMethod, FirJavaValueParameter> = linkedMapOf()
    var valueParameterForValue: Pair<JavaMethod, FirJavaValueParameter>? = null

    inline fun forEach(block: (JavaMethod, FirJavaValueParameter) -> Unit) {
        valueParameterForValue?.let { (javaMethod, firJavaValueParameter) -> block(javaMethod, firJavaValueParameter) }
        valueParameters.forEach { (javaMethod, firJavaValueParameter) -> block(javaMethod, firJavaValueParameter) }
    }
}
