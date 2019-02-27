/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMember
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirModifiableClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.load.java.JavaClassFinder
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

    override fun getCallableSymbols(callableId: CallableId): List<ConeCallableSymbol> {
        return callableCache.lookupCacheOrCalculate(callableId) {
            val classId = callableId.classId ?: return@lookupCacheOrCalculate emptyList()
            val classSymbol = getClassLikeSymbolByFqName(classId) as? FirClassSymbol
                ?: return@lookupCacheOrCalculate emptyList()
            val firClass = classSymbol.fir as FirModifiableClass
            val callableSymbols = mutableListOf<ConeCallableSymbol>()
            for (declaration in firClass.declarations) {
                when (declaration) {
                    is FirCallableMember -> {
                        val declarationId = CallableId(callableId.packageName, callableId.className, declaration.name)
                        if (declarationId == callableId) {
                            val symbol = declaration.symbol as ConeCallableSymbol
                            callableSymbols += symbol
                        }
                    }
                }
            }
            callableSymbols
        }.orEmpty()
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return classCache.lookupCacheOrCalculateWithPostCompute(classId, {
            val foundClass = findClass(classId)
            if (foundClass == null) {
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
                        typeParameters += createTypeParameterSymbol(session, typeParameter.name).fir
                    }
                    addAnnotationsFrom(javaClass)
                    for (supertype in javaClass.supertypes) {
                        superTypeRefs += supertype.toFirResolvedTypeRef(session)
                    }
                    // TODO: may be we can process fields & methods later.
                    // However, they should be built up to override resolve stage
                    for (javaField in javaClass.fields) {
                        if (javaField.isStatic) continue // TODO: statics
                        val fieldName = javaField.name
                        val fieldId = CallableId(classId.packageFqName, classId.relativeClassName, fieldName)
                        val fieldSymbol = FirFieldSymbol(fieldId)
                        val returnType = javaField.type
                        val firJavaField = FirJavaField(
                            session, fieldSymbol, fieldName,
                            javaField.visibility, javaField.modality,
                            returnTypeRef = returnType.toFirJavaTypeRef(session),
                            isVar = !javaField.isFinal
                        ).apply {
                            addAnnotationsFrom(javaField)
                        }
                        declarations += firJavaField
                    }
                    for (javaMethod in javaClass.methods) {
                        if (javaMethod.isStatic) continue // TODO: statics
                        val methodName = javaMethod.name
                        val methodId = CallableId(classId.packageFqName, classId.relativeClassName, methodName)
                        val methodSymbol = FirFunctionSymbol(methodId)
                        val returnType = javaMethod.returnType
                        val firJavaMethod = FirJavaMethod(
                            session, methodSymbol, methodName,
                            javaMethod.visibility, javaMethod.modality,
                            returnTypeRef = returnType.toFirJavaTypeRef(session)
                        ).apply {
                            for (typeParameter in javaMethod.typeParameters) {
                                typeParameters += createTypeParameterSymbol(session, typeParameter.name).fir
                            }
                            addAnnotationsFrom(javaMethod)
                            for (valueParameter in javaMethod.valueParameters) {
                                val parameterType = valueParameter.type
                                valueParameters += FirJavaValueParameter(
                                    session, valueParameter.name ?: Name.special("<anonymous Java parameter>"),
                                    returnTypeRef = parameterType.toFirJavaTypeRef(session),
                                    isVararg = valueParameter.isVararg
                                ).apply {
                                    addAnnotationsFrom(valueParameter)
                                }
                            }
                        }
                        declarations += firJavaMethod
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
}

