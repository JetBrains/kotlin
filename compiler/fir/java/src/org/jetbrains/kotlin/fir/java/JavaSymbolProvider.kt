/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.SymbolProviderCache
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.types.Variance.INVARIANT
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull

class JavaSymbolProvider(
    session: FirSession,
    val project: Project,
    private val searchScope: GlobalSearchScope,
) : FirSymbolProvider(session) {
    companion object {
        private val VALUE_METHOD_NAME = Name.identifier("value")
    }

    private val classCache = SymbolProviderCache<ClassId, FirRegularClassSymbol>()
    private val packageCache = SymbolProviderCache<FqName, FqName>()

    private val scopeProvider = JavaScopeProvider(::wrapScopeWithJvmMapped, this)

    private val facade: KotlinJavaPsiFacade get() = KotlinJavaPsiFacade.getInstance(project)
    private val parentClassTypeParameterStackCache: SymbolProviderCache<FirRegularClassSymbol, JavaTypeParameterStack> = SymbolProviderCache()

    private fun findClass(
        classId: ClassId,
        content: KotlinClassFinder.Result.ClassFileContent?,
    ): JavaClass? = facade.findClass(JavaClassFinder.Request(classId, previouslyFoundClassFileContent = content?.content), searchScope)

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {}

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
        return classCache.lookupCacheOrCalculateWithPostCompute(
            classId,
            {
                val foundClass = findClass(classId, content)
                if (foundClass == null || foundClass.annotations.any {
                        it.classId?.asSingleFqName() == JvmAnnotationNames.METADATA_FQ_NAME
                    }
                ) {
                    null to null
                } else {
                    FirRegularClassSymbol(classId) to foundClass
                }
            },
        ) { firSymbol, foundClass ->
            foundClass?.let { javaClass ->
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
                val firJavaClass = buildJavaClass {
                    source = (javaClass as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
                    session = this@JavaSymbolProvider.session
                    symbol = firSymbol
                    name = javaClass.name
                    visibility = javaClass.visibility.toFirVisibility()
                    modality = javaClass.modality
                    classKind = javaClass.classKind
                    this.isTopLevel = outerClassId == null
                    isStatic = javaClass.isStatic
                    this.javaTypeParameterStack = javaTypeParameterStack
                    parentClassTypeParameterStackCache[firSymbol] = javaTypeParameterStack
                    existingNestedClassifierNames += javaClass.innerClassNames
                    scopeProvider = this@JavaSymbolProvider.scopeProvider
                    val classTypeParameters = foundClass.typeParameters.convertTypeParameters(javaTypeParameterStack)
                    typeParameters += classTypeParameters
                    if (!isStatic && parentClassSymbol != null) {
                        typeParameters += parentClassSymbol.fir.typeParameters.map {
                            buildOuterClassTypeParameterRef { symbol = it.symbol }
                        }
                    }
                    status = FirResolvedDeclarationStatusImpl(
                        javaClass.visibility.toFirVisibility(),
                        javaClass.modality
                    ).apply {
                        this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
                        isCompanion = false
                        isData = false
                        isInline = false
                        isFun = classKind == ClassKind.INTERFACE
                    }
                    addAnnotationsFrom(this@JavaSymbolProvider.session, javaClass, javaTypeParameterStack)
                    // TODO: may be we can process fields & methods later.
                    // However, they should be built up to override resolve stage
                    for (javaField in javaClass.fields) {
                        val fieldName = javaField.name
                        val fieldId = CallableId(classId.packageFqName, classId.relativeClassName, fieldName)
                        val returnType = javaField.type
                        val firJavaDeclaration = when {
                            javaField.isEnumEntry -> buildEnumEntry {
                                source = (javaField as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
                                session = this@JavaSymbolProvider.session
                                symbol = FirVariableSymbol(fieldId)
                                name = fieldName
                                status = FirResolvedDeclarationStatusImpl(
                                    javaField.visibility.toFirVisibility(),
                                    javaField.modality
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
                            }
                            else -> buildJavaField {
                                source = (javaField as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
                                session = this@JavaSymbolProvider.session
                                symbol = FirFieldSymbol(fieldId)
                                name = fieldName
                                status = FirResolvedDeclarationStatusImpl(
                                    javaField.visibility.toFirVisibility(),
                                    javaField.modality
                                ).apply {
                                    isStatic = javaField.isStatic
                                    isExpect = false
                                    isActual = false
                                    isOverride = false
                                }
                                visibility = javaField.visibility.toFirVisibility()
                                modality = javaField.modality
                                returnTypeRef = returnType.toFirJavaTypeRef(this@JavaSymbolProvider.session, javaTypeParameterStack)
                                isVar = !javaField.isFinal
                                isStatic = javaField.isStatic
                                addAnnotationsFrom(this@JavaSymbolProvider.session, javaField, javaTypeParameterStack)
                            }
                        }
                        declarations += firJavaDeclaration
                    }
                    val valueParametersForAnnotationConstructor = mutableListOf<FirJavaValueParameter>()
                    var valueParameterForValueInAnnotationConstructor: FirJavaValueParameter? = null
                    val classIsAnnotation = classKind == ClassKind.ANNOTATION_CLASS

                    for (javaMethod in javaClass.methods) {
                        val methodName = javaMethod.name
                        val methodId = CallableId(classId.packageFqName, classId.relativeClassName, methodName)
                        val methodSymbol = FirNamedFunctionSymbol(methodId)
                        val returnType = javaMethod.returnType
                        val firJavaMethod = buildJavaMethod {
                            session = this@JavaSymbolProvider.session
                            source = (javaMethod as? JavaElementImpl<*>)?.psi?.toFirPsiSourceElement()
                            symbol = methodSymbol
                            name = methodName
                            visibility = javaMethod.visibility.toFirVisibility()
                            modality = javaMethod.modality
                            returnTypeRef = returnType.toFirJavaTypeRef(this@JavaSymbolProvider.session, javaTypeParameterStack)
                            isStatic = javaMethod.isStatic
                            typeParameters += javaMethod.typeParameters.convertTypeParameters(javaTypeParameterStack)
                            addAnnotationsFrom(this@JavaSymbolProvider.session, javaMethod, javaTypeParameterStack)
                            for ((index, valueParameter) in javaMethod.valueParameters.withIndex()) {
                                valueParameters += valueParameter.toFirValueParameter(
                                    this@JavaSymbolProvider.session, index, javaTypeParameterStack,
                                )
                            }
                            status = FirResolvedDeclarationStatusImpl(
                                javaMethod.visibility.toFirVisibility(),
                                javaMethod.modality
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
                        }
                        if (classIsAnnotation) {
                            val parameterForAnnotationConstructor = buildJavaValueParameter {
                                session = this@JavaSymbolProvider.session
                                returnTypeRef = firJavaMethod.returnTypeRef
                                name = firJavaMethod.name
                                if (javaMethod.hasAnnotationParameterDefaultValue) {
                                    defaultValue = buildExpressionStub()
                                }
                                isVararg = javaMethod.returnType is JavaArrayType
                            }
                            if (firJavaMethod.name == VALUE_METHOD_NAME) {
                                valueParameterForValueInAnnotationConstructor = parameterForAnnotationConstructor
                            } else {
                                valueParametersForAnnotationConstructor += parameterForAnnotationConstructor
                            }
                        }
                        declarations += firJavaMethod
                    }
                    val javaClassDeclaredConstructors = javaClass.constructors
                    val constructorId = CallableId(classId.packageFqName, classId.relativeClassName, classId.shortClassName)

                    fun buildSelfTypeRef() = firSymbol.constructType(
                        this@buildJavaClass.typeParameters.map {
                            ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
                        }.toTypedArray(),
                        false,
                    )

                    fun prepareJavaConstructor(
                        visibility: Visibility = this.visibility,
                        psi: PsiElement? = null,
                        isPrimary: Boolean = false,
                    ): FirJavaConstructorBuilder {
                        val constructorSymbol = FirConstructorSymbol(constructorId)
                        return FirJavaConstructorBuilder().apply {
                            source = psi?.toFirPsiSourceElement()
                            session = this@JavaSymbolProvider.session
                            symbol = constructorSymbol
                            isInner = javaClass.outerClass != null && !javaClass.isStatic
                            val isThisInner = this.isInner
                            status = FirResolvedDeclarationStatusImpl(
                                visibility,
                                Modality.FINAL
                            ).apply {
                                isExpect = false
                                isActual = false
                                isOverride = false
                                isInner = isThisInner
                            }
                            this.visibility = visibility
                            this.isPrimary = isPrimary
                            returnTypeRef = buildResolvedTypeRef {
                                type = buildSelfTypeRef()
                            }
                            typeParameters += classTypeParameters.map { buildConstructedClassTypeParameterRef { symbol = it.symbol } }
                        }
                    }

                    if (javaClassDeclaredConstructors.isEmpty()
                        && javaClass.classKind == ClassKind.CLASS
                        && javaClass.hasDefaultConstructor()) {
                        declarations += prepareJavaConstructor(isPrimary = true).build()
                    }
                    for (javaConstructor in javaClassDeclaredConstructors) {
                        declarations += prepareJavaConstructor(
                            visibility = javaConstructor.visibility.toFirVisibility(),
                            psi = (javaConstructor as? JavaElementImpl<*>)?.psi,
                        ).apply {
                            this.typeParameters += javaConstructor.typeParameters.convertTypeParameters(javaTypeParameterStack)
                            addAnnotationsFrom(this@JavaSymbolProvider.session, javaConstructor, javaTypeParameterStack)
                            for ((index, valueParameter) in javaConstructor.valueParameters.withIndex()) {
                                valueParameters += valueParameter.toFirValueParameter(
                                    this@JavaSymbolProvider.session, index, javaTypeParameterStack,
                                )
                            }
                        }.build()
                    }

                    if (classKind == ClassKind.ENUM_CLASS) {
                        generateValuesFunction(session, classId.packageFqName, classId.relativeClassName)
                        generateValueOfFunction(session, classId.packageFqName, classId.relativeClassName)
                    }
                    if (classIsAnnotation) {
                        declarations += buildJavaConstructor {
                            session = this@JavaSymbolProvider.session
                            symbol = FirConstructorSymbol(constructorId)
                            status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
                            returnTypeRef = buildResolvedTypeRef {
                                type = buildSelfTypeRef()
                            }
                            valueParameters.addIfNotNull(valueParameterForValueInAnnotationConstructor)
                            valueParameters += valueParametersForAnnotationConstructor
                            visibility = Visibilities.Public
                            isInner = false
                            isPrimary = true
                        }
                    }
                    parentClassTypeParameterStackCache.remove(firSymbol)
                }
                firJavaClass.replaceSuperTypeRefs(
                    javaClass.supertypes.map { supertype ->
                        supertype.toFirResolvedTypeRef(
                            this@JavaSymbolProvider.session, javaTypeParameterStack, isForSupertypes = true, forTypeParameterBounds = false
                        )
                    }
                )
            }
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        return packageCache.lookupCacheOrCalculate(fqName) {
            try {
                val facade = KotlinJavaPsiFacade.getInstance(project)
                val javaPackage = facade.findPackage(fqName.asString(), searchScope) ?: return@lookupCacheOrCalculate null
                FqName(javaPackage.qualifiedName)
            } catch (e: ProcessCanceledException) {
                return@lookupCacheOrCalculate null
            }
        }
    }

    private val knownClassNamesInPackage = mutableMapOf<FqName, Set<String>?>()

    private fun hasTopLevelClassOf(classId: ClassId): Boolean {
        val knownNames = knownClassNamesInPackage.getOrPut(classId.packageFqName) {
            facade.knownClassNamesInPackage(classId.packageFqName)
        } ?: return true
        return classId.relativeClassName.topLevelName() in knownNames
    }
}

fun FqName.topLevelName() =
    asString().substringBefore(".")


