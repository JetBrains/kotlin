/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaConstructor
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.scopes.JavaClassEnhancementScope
import org.jetbrains.kotlin.fir.java.scopes.JavaClassUseSiteScope
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade

class JavaSymbolProvider(
    val session: FirSession,
    val project: Project,
    private val searchScope: GlobalSearchScope
) : AbstractFirSymbolProvider() {

    private val facade: KotlinJavaPsiFacade get() = KotlinJavaPsiFacade.getInstance(project)

    private fun findClass(classId: ClassId): JavaClass? = facade.findClass(JavaClassFinder.Request(classId), searchScope)

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<ConeCallableSymbol> =
        emptyList()

    override fun getClassDeclaredMemberScope(classId: ClassId): FirScope? {
        val classSymbol = getClassLikeSymbolByFqName(classId) as? FirClassSymbol ?: return null
        return FirClassDeclaredMemberScope(classSymbol.fir)
    }

    override fun getClassUseSiteMemberScope(
        classId: ClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope? {
        val symbol = this.getClassLikeSymbolByFqName(classId) ?: return null
        val javaClass = symbol.firUnsafe<FirJavaClass>()
        return buildJavaEnhancementScope(useSiteSession, javaClass.symbol, scopeSession)
    }

    private fun buildJavaEnhancementScope(
        useSiteSession: FirSession,
        symbol: FirClassSymbol,
        scopeSession: ScopeSession
    ): JavaClassEnhancementScope {
        return scopeSession.getOrBuild(symbol, JAVA_ENHANCEMENT) {
            JavaClassEnhancementScope(useSiteSession, buildJavaUseSiteScope(symbol.fir, useSiteSession, scopeSession))
        }
    }

    private fun buildJavaUseSiteScope(
        regularClass: FirRegularClass,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): JavaClassUseSiteScope {
        return scopeSession.getOrBuild(regularClass.symbol, JAVA_USE_SITE) {
            val superTypeEnhancementScope = FirCompositeScope(mutableListOf())
            val declaredScope = scopeSession.getOrBuild(regularClass.symbol, DECLARED) { FirClassDeclaredMemberScope(regularClass) }
            lookupSuperTypes(regularClass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession)
                .mapNotNullTo(superTypeEnhancementScope.scopes) { useSiteSuperType ->
                    if (useSiteSuperType is ConeClassErrorType) return@mapNotNullTo null
                    val symbol = useSiteSuperType.lookupTag.toSymbol(useSiteSession)
                    if (symbol is FirClassSymbol) {
                        // We need JavaClassEnhancementScope here to have already enhanced signatures from supertypes
                        buildJavaEnhancementScope(useSiteSession, symbol, scopeSession)
                    } else {
                        null
                    }
                }
            JavaClassUseSiteScope(regularClass, useSiteSession, superTypeEnhancementScope, declaredScope)
        }
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        if (!hasTopLevelClassOf(classId)) return null
        return classCache.lookupCacheOrCalculateWithPostCompute(classId, {
            val foundClass = findClass(classId)
            if (foundClass == null || foundClass.annotations.any { it.classId?.asSingleFqName() == JvmAnnotationNames.METADATA_FQ_NAME }) {
                null to null
            } else {
                FirClassSymbol(classId) to foundClass
            }
        }) { firSymbol, foundClass ->
            foundClass?.let { javaClass ->
                FirJavaClass(
                    session, firSymbol as FirClassSymbol, javaClass.name,
                    javaClass.visibility, javaClass.modality,
                    javaClass.classKind,
                    isTopLevel = classId.relativeClassName.parent().isRoot, isStatic = javaClass.isStatic
                ).apply {
                    for (typeParameter in javaClass.typeParameters) {
                        typeParameters += createTypeParameterSymbol(this@JavaSymbolProvider.session, typeParameter.name).fir
                    }
                    addAnnotationsFrom(this@JavaSymbolProvider.session, javaClass)
                    for (supertype in javaClass.supertypes) {
                        superTypeRefs += supertype.toFirResolvedTypeRef(this@JavaSymbolProvider.session)
                    }
                    // TODO: may be we can process fields & methods later.
                    // However, they should be built up to override resolve stage
                    for (javaField in javaClass.fields) {
                        val fieldName = javaField.name
                        val fieldId = CallableId(classId.packageFqName, classId.relativeClassName, fieldName)
                        val fieldSymbol = FirFieldSymbol(fieldId)
                        val returnType = javaField.type
                        val firJavaField = FirJavaField(
                            this@JavaSymbolProvider.session, fieldSymbol, fieldName,
                            javaField.visibility, javaField.modality,
                            returnTypeRef = returnType.toFirJavaTypeRef(this@JavaSymbolProvider.session),
                            isVar = !javaField.isFinal,
                            isStatic = javaField.isStatic
                        ).apply {
                            addAnnotationsFrom(this@JavaSymbolProvider.session, javaField)
                        }
                        declarations += firJavaField
                    }
                    for (javaMethod in javaClass.methods) {
                        val methodName = javaMethod.name
                        val methodId = CallableId(classId.packageFqName, classId.relativeClassName, methodName)
                        val methodSymbol = FirFunctionSymbol(methodId)
                        val returnType = javaMethod.returnType
                        val firJavaMethod = FirJavaMethod(
                            this@JavaSymbolProvider.session, methodSymbol, methodName,
                            javaMethod.visibility, javaMethod.modality,
                            returnTypeRef = returnType.toFirJavaTypeRef(this@JavaSymbolProvider.session),
                            isStatic = javaMethod.isStatic
                        ).apply {
                            for (typeParameter in javaMethod.typeParameters) {
                                typeParameters += createTypeParameterSymbol(this@JavaSymbolProvider.session, typeParameter.name).fir
                            }
                            addAnnotationsFrom(this@JavaSymbolProvider.session, javaMethod)
                            for (valueParameter in javaMethod.valueParameters) {
                                valueParameters += valueParameter.toFirValueParameters(this@JavaSymbolProvider.session)
                            }
                        }
                        declarations += firJavaMethod
                    }
                    for (javaConstructor in javaClass.constructors) {
                        val constructorId = CallableId(classId.packageFqName, classId.relativeClassName, classId.shortClassName)
                        val constructorSymbol = FirFunctionSymbol(constructorId)
                        val classTypeParameters = typeParameters.map {
                            createTypeParameterSymbol(this@JavaSymbolProvider.session, it.name).fir
                        }
                        val constructorTypeParameters = javaConstructor.typeParameters.map {
                            createTypeParameterSymbol(this@JavaSymbolProvider.session, it.name).fir
                        }
                        val typeParameters = classTypeParameters + constructorTypeParameters
                        val firJavaConstructor = FirJavaConstructor(
                            this@JavaSymbolProvider.session, constructorSymbol, javaConstructor.visibility,
                            FirResolvedTypeRefImpl(
                                this@JavaSymbolProvider.session, null,
                                firSymbol.constructType(
                                    classTypeParameters.map { ConeTypeParameterTypeImpl(it.symbol, false) }.toTypedArray(), false
                                ),
                                false, emptyList()
                            )
                        ).apply {
                            this.typeParameters += typeParameters
                            addAnnotationsFrom(this@JavaSymbolProvider.session, javaConstructor)
                            for (valueParameter in javaConstructor.valueParameters) {
                                valueParameters += valueParameter.toFirValueParameters(this@JavaSymbolProvider.session)
                            }
                        }
                        declarations += firJavaConstructor
                    }
                }
            }
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        return packageCache.lookupCacheOrCalculate(fqName) {
            val facade = KotlinJavaPsiFacade.getInstance(project)
            val javaPackage = facade.findPackage(fqName.asString(), searchScope) ?: return@lookupCacheOrCalculate null
            FqName(javaPackage.qualifiedName)
        }
    }

    fun getJavaTopLevelClasses(): List<FirRegularClass> {
        return classCache.values
            .filterIsInstance<FirClassSymbol>()
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


private val JAVA_ENHANCEMENT = scopeSessionKey<JavaClassEnhancementScope>()
private val JAVA_USE_SITE = scopeSessionKey<JavaClassUseSiteScope>()