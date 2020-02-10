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
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.declarations.builder.FirTypeParameterBuilder
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.lazyNestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.types.Variance.INVARIANT

class JavaSymbolProvider(
    val session: FirSession,
    val project: Project,
    private val searchScope: GlobalSearchScope,
) : AbstractFirSymbolProvider<FirRegularClassSymbol>() {

    private val scopeProvider = JavaScopeProvider(::wrapScopeWithJvmMapped, this)

    private val facade: KotlinJavaPsiFacade get() = KotlinJavaPsiFacade.getInstance(project)
    private val parentClassTypeParameterStackCache: MutableMap<FirRegularClassSymbol, JavaTypeParameterStack> = mutableMapOf()

    private fun findClass(
        classId: ClassId,
        content: KotlinClassFinder.Result.ClassFileContent?,
    ): JavaClass? = facade.findClass(JavaClassFinder.Request(classId, previouslyFoundClassFileContent = content?.content), searchScope)

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> =
        emptyList()

    override fun getNestedClassifierScope(classId: ClassId): FirScope? {
        val symbol = this.getClassLikeSymbolByFqName(classId) ?: return null
        val regularClass = symbol.fir
        return if (regularClass is FirJavaClass) {
            lazyNestedClassifierScope(
                classId,
                existingNames = regularClass.existingNestedClassifierNames,
                symbolProvider = this,
            )
        } else {
            nestedClassifierScope(regularClass)
        }
    }

    private fun JavaTypeParameter.toFirTypeParameter(javaTypeParameterStack: JavaTypeParameterStack): FirTypeParameterBuilder {
        javaTypeParameterStack.getBuilder(this)?.let { return it }
        val firSymbol = FirTypeParameterSymbol()
        return FirTypeParameterBuilder().apply {
            session = this@JavaSymbolProvider.session
            name = this@toFirTypeParameter.name
            symbol = firSymbol
            variance = INVARIANT
            isReified = false
        }.also {
            javaTypeParameterStack.add(this, it)
        }
    }

    private fun FirTypeParameterBuilder.addBounds(
        javaTypeParameter: JavaTypeParameter,
        stack: JavaTypeParameterStack,
    ) {
        for (upperBound in javaTypeParameter.upperBounds) {
            bounds += upperBound.toFirResolvedTypeRef(this@JavaSymbolProvider.session, stack, nullability = ConeNullability.NULLABLE)
        }
        addDefaultBoundIfNecessary()
    }

    private fun List<JavaTypeParameter>.convertTypeParameters(stack: JavaTypeParameterStack): List<FirTypeParameter> {
        return this
            .map { it.toFirTypeParameter(stack) }
            .mapIndexed { index, typeParameterBuilder ->
                if (typeParameterBuilder.bounds.isEmpty()) {
                    typeParameterBuilder.addBounds(this[index], stack)
                }
                typeParameterBuilder.build()
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
                if (foundClass == null || foundClass.annotations.any { it.classId?.asSingleFqName() == JvmAnnotationNames.METADATA_FQ_NAME }) {
                    null to null
                } else {
                    FirRegularClassSymbol(classId) to foundClass
                }
            },
        ) { firSymbol, foundClass ->
            foundClass?.let { javaClass ->
                val javaTypeParameterStack = JavaTypeParameterStack()
                val parentFqName = classId.relativeClassName.parent()
                val isTopLevel = parentFqName.isRoot
                if (!isTopLevel) {
                    val parentId = ClassId(classId.packageFqName, parentFqName, false)
                    val parentClassSymbol = getClassLikeSymbolByFqName(parentId)
                    val parentStack = parentClassTypeParameterStackCache[parentClassSymbol] ?:
                        (parentClassSymbol?.fir as? FirJavaClass)?.javaTypeParameterStack
                    if (parentStack != null) {
                        javaTypeParameterStack.addStack(parentStack)
                    }
                }
                buildJavaClass {
                    source = (javaClass as? JavaElementImpl<*>)?.psi?.toFirSourceElement()
                    session = this@JavaSymbolProvider.session
                    symbol = firSymbol
                    name = javaClass.name
                    visibility = javaClass.visibility
                    modality = javaClass.modality
                    classKind = javaClass.classKind
                    this.isTopLevel = isTopLevel
                    isStatic = javaClass.isStatic
                    this.javaTypeParameterStack = javaTypeParameterStack
                    parentClassTypeParameterStackCache[firSymbol] = javaTypeParameterStack
                    existingNestedClassifierNames += javaClass.innerClassNames
                    scopeProvider = this@JavaSymbolProvider.scopeProvider
                    typeParameters += foundClass.typeParameters.convertTypeParameters(javaTypeParameterStack)
                    addAnnotationsFrom(this@JavaSymbolProvider.session, javaClass, javaTypeParameterStack)
                    for (supertype in javaClass.supertypes) {
                        superTypeRefs += supertype.toFirResolvedTypeRef(
                            this@JavaSymbolProvider.session, javaTypeParameterStack, typeParametersNullability = ConeNullability.UNKNOWN
                        )
                    }
                    // TODO: may be we can process fields & methods later.
                    // However, they should be built up to override resolve stage
                    for (javaField in javaClass.fields) {
                        val fieldName = javaField.name
                        val fieldId = CallableId(classId.packageFqName, classId.relativeClassName, fieldName)
                        val fieldSymbol = FirFieldSymbol(fieldId)
                        val returnType = javaField.type
                        val firJavaField = buildJavaField {
                            source = (javaField as? JavaElementImpl<*>)?.psi?.toFirSourceElement()
                            session = this@JavaSymbolProvider.session
                            symbol = fieldSymbol
                            name = fieldName
                            visibility = javaField.visibility
                            modality = javaField.modality
                            returnTypeRef = returnType.toFirJavaTypeRef(this@JavaSymbolProvider.session, javaTypeParameterStack)
                            isVar = !javaField.isFinal
                            isStatic = javaField.isStatic
                            addAnnotationsFrom(this@JavaSymbolProvider.session, javaField, javaTypeParameterStack)
                        }
                        declarations += firJavaField
                    }
                    for (javaMethod in javaClass.methods) {
                        val methodName = javaMethod.name
                        val methodId = CallableId(classId.packageFqName, classId.relativeClassName, methodName)
                        val methodSymbol = FirNamedFunctionSymbol(methodId)
                        val returnType = javaMethod.returnType
                        val firJavaMethod = buildJavaMethod {
                            session = this@JavaSymbolProvider.session
                            source = (javaMethod as? JavaElementImpl<*>)?.psi?.toFirSourceElement()
                            symbol = methodSymbol
                            name = methodName
                            visibility = javaMethod.visibility
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
                        }
                        declarations += firJavaMethod
                    }
                    val javaClassDeclaredConstructors = javaClass.constructors
                    val constructorId = CallableId(classId.packageFqName, classId.relativeClassName, classId.shortClassName)

                    fun prepareJavaConstructor(
                        visibility: Visibility = this.visibility,
                        psi: PsiElement? = null,
                        isPrimary: Boolean = false,
                    ): FirJavaConstructorBuilder {
                        val constructorSymbol = FirConstructorSymbol(constructorId)
                        val classTypeParameters = javaClass.typeParameters.convertTypeParameters(javaTypeParameterStack)
                        val firJavaConstructor = FirJavaConstructorBuilder().apply {
                            source = psi?.toFirSourceElement()
                            session = this@JavaSymbolProvider.session
                            symbol = constructorSymbol
                            this.visibility = visibility
                            this.isPrimary = isPrimary
                            isInner = !javaClass.isStatic
                            returnTypeRef = buildResolvedTypeRef {
                                type = firSymbol.constructType(
                                    classTypeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                                    false,
                                )
                            }
                            typeParameters += classTypeParameters
                        }
                        return firJavaConstructor
                    }

                    if (javaClassDeclaredConstructors.isEmpty() && javaClass.classKind == ClassKind.CLASS) {
                        declarations += prepareJavaConstructor(isPrimary = true).build()
                    }
                    for (javaConstructor in javaClassDeclaredConstructors) {
                        declarations += prepareJavaConstructor(
                            visibility = javaConstructor.visibility, psi = (javaConstructor as? JavaElementImpl<*>)?.psi,
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
                    isNotSam = isNotSam()
                    parentClassTypeParameterStackCache.remove(firSymbol)
                }
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

    fun getJavaTopLevelClasses(): List<FirRegularClass> {
        return classCache.values
            .filterIsInstance<FirRegularClassSymbol>()
            .filter { it.classId.relativeClassName.parent().isRoot }
            .map { it.fir }
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


