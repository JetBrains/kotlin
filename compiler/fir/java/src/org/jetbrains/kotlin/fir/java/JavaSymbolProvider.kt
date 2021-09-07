/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
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
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.enhancement.FirSignatureEnhancement
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.load.java.structure.impl.JavaPackageImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.types.Variance.INVARIANT
import org.jetbrains.kotlin.util.OperatorNameConventions

@ThreadSafeMutableState
class JavaSymbolProvider(
    session: FirSession,
    val baseModuleData: FirModuleData,
    val project: Project,
    private val searchScope: GlobalSearchScope,
) : FirSymbolProvider(session) {
    companion object {
        val VALUE_METHOD_NAME = Name.identifier("value")
    }

    private val classCache =
        session.firCachesFactory.createCacheWithPostCompute<ClassId, FirRegularClassSymbol?, Nothing?, JavaClass?>(
            createValue = { classId, _ ->
                val foundClass = findClass(classId)
                if (foundClass == null) {
                    null to null
                } else {
                    FirRegularClassSymbol(classId) to foundClass
                }
            },
            postCompute = { _, classSymbol, javaClass ->
                if (classSymbol != null && javaClass != null) {
                    convertJavaClassToFir(classSymbol, javaClass)
                }
            }
        )

    private val packageCache = session.firCachesFactory.createCache(::findPackage)
    private val knownClassNamesInPackage = session.firCachesFactory.createCache<FqName, Set<String>?>(::getKnownClassNames)

    private val scopeProvider = JavaScopeProvider(this)

    private val facade: KotlinJavaPsiFacade get() = KotlinJavaPsiFacade.getInstance(project)
    private val parentClassTypeParameterStackCache = mutableMapOf<FirRegularClassSymbol, JavaTypeParameterStack>()
    private val parentClassEffectiveVisibilityCache = mutableMapOf<FirRegularClassSymbol, EffectiveVisibility>()

    private fun findClass(classId: ClassId): JavaClass? =
        facade.findClass(JavaClassFinder.Request(classId), searchScope)
            ?.takeIf { !it.hasDifferentClassId(classId) && !it.hasMetadataAnnotation() }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    private fun JavaTypeParameter.toFirTypeParameter(javaTypeParameterStack: JavaTypeParameterStack): FirTypeParameter {
        return buildTypeParameter {
            moduleData = this@JavaSymbolProvider.baseModuleData
            origin = FirDeclarationOrigin.Java
            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
            name = this@toFirTypeParameter.name
            symbol = FirTypeParameterSymbol()
            variance = INVARIANT
            isReified = false
            javaTypeParameterStack.addParameter(this@toFirTypeParameter, symbol)
            // TODO: should be lazy (in case annotations refer to the containing class)
            annotations.addFromJava(session, this@toFirTypeParameter, javaTypeParameterStack)
            for (upperBound in this@toFirTypeParameter.upperBounds) {
                bounds += upperBound.toFirJavaTypeRef(session, javaTypeParameterStack)
            }
            if (bounds.isEmpty()) {
                val builtinTypes = baseModuleData.session.builtinTypes
                bounds += buildResolvedTypeRef {
                    type = ConeFlexibleType(builtinTypes.anyType.type, builtinTypes.nullableAnyType.type)
                }
            }
        }
    }

    private fun List<JavaTypeParameter>.convertTypeParameters(stack: JavaTypeParameterStack): List<FirTypeParameter> =
        map { it.toFirTypeParameter(stack) }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
        return try {
            if (!hasTopLevelClassOf(classId)) return null
            getFirJavaClass(classId)
        } catch (e: ProcessCanceledException) {
            null
        }
    }

    private fun getFirJavaClass(classId: ClassId): FirRegularClassSymbol? {
        // Enforce loading of outer class first
        classId.outerClassId?.let { getFirJavaClass(it) }
        return classCache.getValue(classId, null)
    }

    private fun JavaClass.hasDifferentClassId(lookupClassId: ClassId): Boolean =
        classId != lookupClassId

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

    private fun convertJavaClassToFir(classSymbol: FirRegularClassSymbol, javaClass: JavaClass): FirJavaClass {
        val classId = classSymbol.classId
        val javaTypeParameterStack = JavaTypeParameterStack()
        val outerClassId = classId.outerClassId
        val parentClassSymbol = if (outerClassId != null) {
            getClassLikeSymbolByClassId(outerClassId)
        } else null


        if (parentClassSymbol != null) {
            val parentStack = parentClassTypeParameterStackCache[parentClassSymbol]
                ?: (parentClassSymbol.fir as? FirJavaClass)?.javaTypeParameterStack
            if (parentStack != null) {
                javaTypeParameterStack.addStack(parentStack)
            }
        }
        parentClassTypeParameterStackCache[classSymbol] = javaTypeParameterStack
        val firJavaClass = createFirJavaClass(javaClass, classSymbol, outerClassId, parentClassSymbol, classId, javaTypeParameterStack)
        parentClassTypeParameterStackCache.remove(classSymbol)
        parentClassEffectiveVisibilityCache.remove(classSymbol)

        // There's a bit of an ordering restriction here:
        // 1. annotations should be added after the symbol is bound, as annotations can refer to the class itself;
        // 2. type enhancement requires annotations to be already present (and supertypes can refer to type parameters).
        firJavaClass.annotations.addFromJava(session, javaClass, javaTypeParameterStack)
        val enhancement = FirSignatureEnhancement(firJavaClass, session) { emptyList() }
        enhancement.enhanceTypeParameterBounds(firJavaClass.typeParameters)
        firJavaClass.superTypeRefs.replaceAll { enhancement.enhanceSuperType(it) }
        firJavaClass.replaceDeprecation(firJavaClass.getDeprecationInfos(session.languageVersionSettings.apiVersion))
        return firJavaClass
    }

    private fun createFirJavaClass(
        javaClass: JavaClass,
        classSymbol: FirRegularClassSymbol,
        outerClassId: ClassId?,
        parentClassSymbol: FirRegularClassSymbol?,
        classId: ClassId,
        javaTypeParameterStack: JavaTypeParameterStack,
    ): FirJavaClass {
        val valueParametersForAnnotationConstructor = ValueParametersForAnnotationConstructor()
        val classIsAnnotation = javaClass.classKind == ClassKind.ANNOTATION_CLASS
        return buildJavaClass {
            source = (javaClass as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
            moduleData = this@JavaSymbolProvider.baseModuleData
            symbol = classSymbol
            name = javaClass.name
            val visibility = javaClass.visibility
            this@buildJavaClass.visibility = visibility
            classKind = javaClass.classKind
            modality = if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.ENUM_CLASS) Modality.FINAL else javaClass.modality
            this.isTopLevel = outerClassId == null
            isStatic = javaClass.isStatic
            javaPackage = packageCache.getValue(classSymbol.classId.packageFqName)
            this.javaTypeParameterStack = javaTypeParameterStack
            existingNestedClassifierNames += javaClass.innerClassNames
            scopeProvider = this@JavaSymbolProvider.scopeProvider

            val selfEffectiveVisibility = visibility.toEffectiveVisibility(parentClassSymbol?.toLookupTag(), forClass = true)
            val parentEffectiveVisibility = parentClassSymbol?.let {
                parentClassEffectiveVisibilityCache[it] ?: it.fir.effectiveVisibility
            } ?: EffectiveVisibility.Public
            val effectiveVisibility = parentEffectiveVisibility.lowerBound(selfEffectiveVisibility, session.typeContext)
            parentClassEffectiveVisibilityCache[classSymbol] = effectiveVisibility

            val classTypeParameters = javaClass.typeParameters.convertTypeParameters(javaTypeParameterStack)
            typeParameters += classTypeParameters
            if (!isStatic && parentClassSymbol != null) {
                typeParameters += parentClassSymbol.fir.typeParameters.map {
                    buildOuterClassTypeParameterRef { symbol = it.symbol }
                }
            }
            javaClass.supertypes.mapTo(superTypeRefs) { it.toFirJavaTypeRef(session, javaTypeParameterStack) }

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
                declarations += convertJavaFieldToFir(javaField, classId, javaTypeParameterStack, dispatchReceiver)
            }

            for (javaMethod in javaClass.methods) {
                if (javaMethod.isObjectMethodInInterface()) continue
                val firJavaMethod = convertJavaMethodToFir(
                    javaMethod,
                    classId,
                    javaTypeParameterStack,
                    dispatchReceiver
                )
                declarations += firJavaMethod

                if (classIsAnnotation) {
                    val parameterForAnnotationConstructor = convertJavaAnnotationMethodToValueParameter(javaMethod, firJavaMethod)
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
                    javaTypeParameterStack
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
                )
            }

            if (classKind == ClassKind.ENUM_CLASS) {
                generateValuesFunction(
                    baseModuleData,
                    classId.packageFqName,
                    classId.relativeClassName
                )
                generateValueOfFunction(baseModuleData, classId.packageFqName, classId.relativeClassName)
            }
            if (classIsAnnotation) {
                declarations +=
                    buildConstructorForAnnotationClass(
                        classSource = (javaClass as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement(FirFakeSourceElementKind.ImplicitConstructor) as? FirFakeSourceElement,
                        constructorId = constructorId,
                        ownerClassBuilder = this,
                        valueParametersForAnnotationConstructor = valueParametersForAnnotationConstructor
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
        dispatchReceiver: ConeClassLikeType
    ): FirDeclaration {
        val fieldName = javaField.name
        val fieldId = CallableId(classId.packageFqName, classId.relativeClassName, fieldName)
        val returnType = javaField.type
        return when {
            javaField.isEnumEntry -> buildEnumEntry {
                source = (javaField as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
                moduleData = this@JavaSymbolProvider.baseModuleData
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
                returnTypeRef = returnType.toFirJavaTypeRef(this@JavaSymbolProvider.session, javaTypeParameterStack)
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                origin = FirDeclarationOrigin.Java
                // TODO: check if this works properly with annotations that take the enum class as an argument
                annotations.addFromJava(this@JavaSymbolProvider.session, javaField, javaTypeParameterStack)
            }.apply {
                containingClassForStaticMemberAttr = ConeClassLikeLookupTagImpl(classId)
            }
            else -> buildJavaField {
                source = (javaField as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
                moduleData = this@JavaSymbolProvider.baseModuleData
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
                returnTypeRef = returnType.toFirJavaTypeRef(this@JavaSymbolProvider.session, javaTypeParameterStack)
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
        dispatchReceiver: ConeClassLikeType
    ): FirJavaMethod {
        val methodName = javaMethod.name
        val methodId = CallableId(classId.packageFqName, classId.relativeClassName, methodName)
        val methodSymbol = FirNamedFunctionSymbol(methodId)
        val returnType = javaMethod.returnType
        return buildJavaMethod {
            moduleData = this@JavaSymbolProvider.baseModuleData
            source = (javaMethod as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
            symbol = methodSymbol
            name = methodName
            returnTypeRef = returnType.toFirJavaTypeRef(this@JavaSymbolProvider.session, javaTypeParameterStack)
            isStatic = javaMethod.isStatic
            typeParameters += javaMethod.typeParameters.convertTypeParameters(javaTypeParameterStack)
            for ((index, valueParameter) in javaMethod.valueParameters.withIndex()) {
                valueParameters += valueParameter.toFirValueParameter(
                    this@JavaSymbolProvider.session, moduleData, index, javaTypeParameterStack,
                )
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

    private fun convertJavaAnnotationMethodToValueParameter(javaMethod: JavaMethod, firJavaMethod: FirJavaMethod): FirJavaValueParameter =
        buildJavaValueParameter {
            source = (javaMethod as? JavaElementImpl<*>)?.psi
                ?.toFirPsiSourceElement(FirFakeSourceElementKind.ImplicitJavaAnnotationConstructor)
            moduleData = this@JavaSymbolProvider.baseModuleData
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
    ): FirJavaConstructor {
        val constructorSymbol = FirConstructorSymbol(constructorId)
        return buildJavaConstructor {
            source = (javaConstructor as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
            moduleData = this@JavaSymbolProvider.baseModuleData
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
            typeParameters += classTypeParameters.map { buildConstructedClassTypeParameterRef { symbol = it.symbol } }

            if (javaConstructor != null) {
                this.typeParameters += javaConstructor.typeParameters.convertTypeParameters(javaTypeParameterStack)
                annotationBuilder = { javaConstructor.convertAnnotationsToFir(session, javaTypeParameterStack) }
                for ((index, valueParameter) in javaConstructor.valueParameters.withIndex()) {
                    valueParameters += valueParameter.toFirValueParameter(
                        this@JavaSymbolProvider.session, moduleData, index, javaTypeParameterStack,
                    )
                }
            } else {
                annotationBuilder = { emptyList() }
            }
        }.apply {
            containingClassForStaticMemberAttr = ownerClassBuilder.symbol.toLookupTag()
        }
    }

    private fun buildConstructorForAnnotationClass(
        classSource: FirFakeSourceElement?,
        constructorId: CallableId,
        ownerClassBuilder: FirJavaClassBuilder,
        valueParametersForAnnotationConstructor: ValueParametersForAnnotationConstructor
    ): FirJavaConstructor {
        return buildJavaConstructor {
            source = classSource
            moduleData = this@JavaSymbolProvider.baseModuleData
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

    override fun getPackage(fqName: FqName): FqName? {
        return packageCache.getValue(fqName)?.fqName
    }

    private fun findPackage(fqName: FqName): JavaPackage? {
        return try {
            val facade = KotlinJavaPsiFacade.getInstance(project)
            val javaPackage = facade.findPackage(fqName.asString(), searchScope) ?: return null
            JavaPackageImpl(javaPackage, searchScope)
        } catch (e: ProcessCanceledException) {
            return null
        }
    }

    private fun hasTopLevelClassOf(classId: ClassId): Boolean {
        val knownNames = knownClassNamesInPackage.getValue(classId.packageFqName) ?: return true
        return classId.relativeClassName.topLevelName() in knownNames
    }

    private fun getKnownClassNames(packageFqName: FqName): MutableSet<String>? =
        facade.knownClassNamesInPackage(packageFqName, searchScope)
}

fun FqName.topLevelName() =
    asString().substringBefore(".")
