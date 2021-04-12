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
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
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
    val project: Project,
    private val searchScope: GlobalSearchScope,
) : FirSymbolProvider(session) {
    companion object {
        internal val VALUE_METHOD_NAME = Name.identifier("value")
    }

    private val classCache =
        session.firCachesFactory.createCacheWithPostCompute(
            createValue = ::findAndConvertJavaClass,
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

    private fun findClass(
        classId: ClassId,
        content: KotlinClassFinder.Result.ClassFileContent?,
    ): JavaClass? = facade.findClass(JavaClassFinder.Request(classId, previouslyFoundClassFileContent = content?.content), searchScope)

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    private fun JavaTypeParameter.toFirTypeParameterSymbol(
        javaTypeParameterStack: JavaTypeParameterStack
    ): Pair<FirTypeParameterSymbol, Boolean> {
        val stored = javaTypeParameterStack.safeGet(this)
        if (stored != null) return stored to true
        val firSymbol = FirTypeParameterSymbol()
        javaTypeParameterStack.addParameter(this, firSymbol)
        return firSymbol to false
    }

    private fun JavaTypeParameter.toFirTypeParameter(
        firSymbol: FirTypeParameterSymbol,
        javaTypeParameterStack: JavaTypeParameterStack
    ): FirTypeParameter {
        return FirTypeParameterBuilder().apply {
            this.session = this@JavaSymbolProvider.session
            origin = FirDeclarationOrigin.Java
            this.name = this@toFirTypeParameter.name
            symbol = firSymbol
            variance = INVARIANT
            isReified = false
            addBounds(this@toFirTypeParameter, javaTypeParameterStack)
        }.build()
    }

    private fun FirTypeParameterBuilder.addBounds(
        javaTypeParameter: JavaTypeParameter,
        stack: JavaTypeParameterStack
    ) {
        for (upperBound in javaTypeParameter.upperBounds) {
            bounds += upperBound.toFirResolvedTypeRef(
                this@JavaSymbolProvider.session,
                stack,
                isForSupertypes = false,
                forTypeParameterBounds = true
            )
        }
        addDefaultBoundIfNecessary(isFlexible = true)
    }

    private fun List<JavaTypeParameter>.convertTypeParameters(stack: JavaTypeParameterStack): List<FirTypeParameter> {
        return map { it.toFirTypeParameterSymbol(stack) }.mapIndexed { index, (symbol, initialized) ->
            // This nasty logic is required, because type parameter bound can refer other type parameter from the list
            // So we have to create symbols first, and type parameters themselves after them
            if (initialized) symbol.fir
            else this[index].toFirTypeParameter(symbol, stack)
        }
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirRegularClassSymbol? {
        return try {
            getFirJavaClass(classId)
        } catch (e: ProcessCanceledException) {
            null
        }
    }

    fun getFirJavaClass(classId: ClassId, content: KotlinClassFinder.Result.ClassFileContent? = null): FirRegularClassSymbol? {
        if (!hasTopLevelClassOf(classId)) return null
        return classCache.getValue(classId, content)
    }

    private fun findAndConvertJavaClass(
        classId: ClassId,
        content: KotlinClassFinder.Result.ClassFileContent?
    ): Pair<FirRegularClassSymbol?, JavaClass?> {
        val foundClass = findClass(classId, content)
        return if (foundClass == null ||
            foundClass.hasDifferentClassId(classId) ||
            foundClass.hasMetadataAnnotation()
        ) {
            null to null
        } else {
            val symbol = FirRegularClassSymbol(classId)
            symbol to foundClass
        }
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
            getClassLikeSymbolByFqName(outerClassId)
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
        firJavaClass.convertSuperTypes(javaClass, javaTypeParameterStack)
        firJavaClass.addAnnotationsFrom(this@JavaSymbolProvider.session, javaClass, javaTypeParameterStack)
        return firJavaClass
    }

    private fun FirJavaClass.convertSuperTypes(
        javaClass: JavaClass,
        javaTypeParameterStack: JavaTypeParameterStack
    ) {
        replaceSuperTypeRefs(
            javaClass.supertypes.map { supertype ->
                supertype.toFirResolvedTypeRef(session, javaTypeParameterStack, isForSupertypes = true, forTypeParameterBounds = false)
            }
        )
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
            session = this@JavaSymbolProvider.session
            symbol = classSymbol
            name = javaClass.name
            val visibility = javaClass.visibility
            this@buildJavaClass.visibility = visibility
            modality = javaClass.modality
            classKind = javaClass.classKind
            this.isTopLevel = outerClassId == null
            isStatic = javaClass.isStatic
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

            val dispatchReceiver = classId.defaultType(typeParameters.map { it.symbol })

            status = FirResolvedDeclarationStatusImpl(
                visibility,
                javaClass.modality,
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
                    session,
                    classId.packageFqName,
                    classId.relativeClassName
                )
                generateValueOfFunction(session, classId.packageFqName, classId.relativeClassName)
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
                session = this@JavaSymbolProvider.session
                symbol = FirVariableSymbol(fieldId)
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
                addAnnotationsFrom(this@JavaSymbolProvider.session, javaField, javaTypeParameterStack)
            }.apply {
                containingClassAttr = ConeClassLikeLookupTagImpl(classId)
            }
            else -> buildJavaField {
                source = (javaField as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
                session = this@JavaSymbolProvider.session
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
                annotationBuilder = { javaField.annotations.map { it.toFirAnnotationCall(session, javaTypeParameterStack) } }
                initializer = convertJavaInitializerToFir(javaField.initializerValue)

                if (!javaField.isStatic) {
                    dispatchReceiverType = dispatchReceiver
                }
            }.apply {
                if (javaField.isStatic) {
                    containingClassAttr = ConeClassLikeLookupTagImpl(classId)
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
            session = this@JavaSymbolProvider.session
            source = (javaMethod as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
            symbol = methodSymbol
            name = methodName
            visibility = javaMethod.visibility
            modality = javaMethod.modality
            returnTypeRef = returnType.toFirJavaTypeRef(this@JavaSymbolProvider.session, javaTypeParameterStack)
            isStatic = javaMethod.isStatic
            typeParameters += javaMethod.typeParameters.convertTypeParameters(javaTypeParameterStack)
            for ((index, valueParameter) in javaMethod.valueParameters.withIndex()) {
                valueParameters += valueParameter.toFirValueParameter(
                    this@JavaSymbolProvider.session, index, javaTypeParameterStack,
                )
            }
            annotationBuilder = { javaMethod.annotations.map { it.toFirAnnotationCall(session, javaTypeParameterStack) } }
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
                containingClassAttr = ConeClassLikeLookupTagImpl(classId)
            }
        }
    }

    private fun convertJavaAnnotationMethodToValueParameter(javaMethod: JavaMethod, firJavaMethod: FirJavaMethod): FirJavaValueParameter =
        buildJavaValueParameter {
            source = (javaMethod as? JavaElementImpl<*>)?.psi
                ?.toFirPsiSourceElement(FirFakeSourceElementKind.ImplicitJavaAnnotationConstructor)
            session = this@JavaSymbolProvider.session
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
            session = this@JavaSymbolProvider.session
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
                annotationBuilder = { javaConstructor.annotations.map { it.toFirAnnotationCall(session, javaTypeParameterStack) } }
                for ((index, valueParameter) in javaConstructor.valueParameters.withIndex()) {
                    valueParameters += valueParameter.toFirValueParameter(
                        this@JavaSymbolProvider.session, index, javaTypeParameterStack,
                    )
                }
            } else {
                annotationBuilder = { emptyList() }
            }
        }.apply {
            containingClassAttr = ownerClassBuilder.symbol.toLookupTag()
        }
    }

    private fun buildConstructorForAnnotationClass(
        classSource: FirFakeSourceElement<*>?,
        constructorId: CallableId,
        ownerClassBuilder: FirJavaClassBuilder,
        valueParametersForAnnotationConstructor: ValueParametersForAnnotationConstructor
    ): FirJavaConstructor {
        return buildJavaConstructor {
            source = classSource
            session = this@JavaSymbolProvider.session
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
            containingClassAttr = ownerClassBuilder.symbol.toLookupTag()
        }
    }

    private fun FirJavaClassBuilder.buildSelfTypeRef(): ConeKotlinType = symbol.constructType(
        typeParameters.map {
            ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), isNullable = false)
        }.toTypedArray(),
        isNullable = false,
    )

    override fun getPackage(fqName: FqName): FqName? {
        return packageCache.getValue(fqName)
    }

    private fun findPackage(fqName: FqName): FqName? {
        return try {
            val facade = KotlinJavaPsiFacade.getInstance(project)
            val javaPackage = facade.findPackage(fqName.asString(), searchScope) ?: return null
            FqName(javaPackage.qualifiedName)
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
