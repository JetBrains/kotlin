/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.types.Variance

class JavaSymbolProvider(
    val session: FirSession,
    val project: Project,
    private val searchScope: GlobalSearchScope
) : AbstractFirSymbolProvider() {

    private val facade: KotlinJavaPsiFacade get() = KotlinJavaPsiFacade.getInstance(project)

    private fun JavaAnnotation.toFirAnnotationCall(): FirAnnotationCall {
        return FirAnnotationCallImpl(
            session, psi = null, useSiteTarget = null,
            annotationTypeRef = FirResolvedTypeRefImpl(
                session = session,
                psi = null,
                type = ConeClassTypeImpl(FirClassSymbol(classId!!), emptyArray()),
                isMarkedNullable = true,
                annotations = emptyList()
            )
        ).apply {
            for (argument in this@toFirAnnotationCall.arguments) {
                arguments += argument.toFirExpression()
            }
        }
    }

    private fun JavaAnnotationArgument.toFirExpression(): FirExpression {
        // TODO: this.name
        return when (this) {
            is JavaLiteralAnnotationArgument -> when (value) {
                null -> FirConstExpressionImpl(session, null, IrConstKind.Null, null)
                else -> FirErrorExpressionImpl(session, null, "Unknown value in JavaLiteralAnnotationArgument: $value")
            }
            is JavaArrayAnnotationArgument -> FirArrayOfCallImpl(session, null).apply {
                for (element in getElements()) {
                    arguments += element.toFirExpression()
                }
            }
            // TODO
            //is JavaEnumValueAnnotationArgument -> {}
            is JavaClassObjectAnnotationArgument -> FirGetClassCallImpl(session, null).apply {
                // TODO
                //arguments += getReferencedType().toFirType()
            }
            is JavaAnnotationAsAnnotationArgument -> getAnnotation().toFirAnnotationCall()
            else -> FirErrorExpressionImpl(session, null, "Unknown JavaAnnotationArgument: ${this::class.java}")
        }
    }

    private fun JavaClassifierType.toFirResolvedTypeRef(): FirResolvedTypeRef {
        val coneType = when (val classifier = classifier) {
            is JavaClass -> {
                val symbol = session.service<FirSymbolProvider>().getClassLikeSymbolByFqName(classifier.classId!!) as ConeClassSymbol
                ConeClassTypeImpl(symbol, typeArguments = typeArguments.map { it.toConeProjection() }.toTypedArray())
            }
            is JavaTypeParameter -> {
                // TODO: it's unclear how to identify type parameter by the symbol
                // TODO: some type parameter cache (provider?)
                val symbol = createTypeParameterSymbol(classifier.name)
                ConeTypeParameterTypeImpl(symbol)
            }
            else -> ConeClassErrorType(reason = "Unexpected classifier: $classifier")
        }
        return FirResolvedTypeRefImpl(
            session, psi = null, type = coneType,
            isMarkedNullable = false, annotations = annotations.map { it.toFirAnnotationCall() }
        )
    }

    private fun JavaType.toFirResolvedTypeRef(): FirResolvedTypeRef {
        if (this is JavaClassifierType) return toFirResolvedTypeRef()
        return FirResolvedTypeRefImpl(
            session, psi = null, type = ConeClassErrorType("Unexpected JavaType: $this"),
            isMarkedNullable = false, annotations = emptyList()
        )
    }

    private fun JavaType.toConeProjection(): ConeKotlinTypeProjection {
        if (this is JavaClassifierType) {
            return toFirResolvedTypeRef().type
        }
        return ConeClassErrorType("Unexpected type argument: $this")
    }

    private fun createTypeParameterSymbol(name: Name): FirTypeParameterSymbol {
        val firSymbol = FirTypeParameterSymbol()
        FirTypeParameterImpl(session, null, firSymbol, name, variance = Variance.INVARIANT, isReified = false)
        return firSymbol
    }

    private fun FirAbstractAnnotatedElement.addAnnotationsFrom(javaAnnotationOwner: JavaAnnotationOwner) {
        for (annotation in javaAnnotationOwner.annotations) {
            annotations += annotation.toFirAnnotationCall()
        }
    }

    private fun findClass(classId: ClassId): JavaClass? = facade.findClass(JavaClassFinder.Request(classId), searchScope)

    override fun getCallableSymbols(callableId: CallableId): List<ConeCallableSymbol> {
        return callableCache.lookupCacheOrCalculate(callableId) {
            val classId = callableId.classId ?: return@lookupCacheOrCalculate emptyList()
            val classSymbol = getClassLikeSymbolByFqName(classId) as? FirClassSymbol
                ?: return@lookupCacheOrCalculate emptyList()
            val firClass = classSymbol.fir as FirModifiableClass
            val callableSymbols = mutableListOf<ConeCallableSymbol>()
            findClass(classId)?.let { javaClass ->
                if (firClass.declarations.isEmpty()) {
                    for (javaMethod in javaClass.methods) {
                        val methodName = javaMethod.name
                        val methodId = CallableId(callableId.packageName, callableId.className, methodName)
                        val methodSymbol = FirFunctionSymbol(methodId)
                        val memberFunction = FirMemberFunctionImpl(
                            session, null, methodSymbol, methodName,
                            javaMethod.visibility, javaMethod.modality,
                            isExpect = false, isActual = false, isOverride = false,
                            isOperator = true, isInfix = false, isInline = false,
                            isTailRec = false, isExternal = false, isSuspend = false,
                            receiverTypeRef = null, returnTypeRef = javaMethod.returnType.toFirResolvedTypeRef()
                        ).apply {
                            for (typeParameter in javaMethod.typeParameters) {
                                typeParameters += createTypeParameterSymbol(typeParameter.name).fir
                            }
                            addAnnotationsFrom(javaMethod)
                            for (valueParameter in javaMethod.valueParameters) {
                                valueParameters += FirValueParameterImpl(
                                    session, null, valueParameter.name ?: Name.special("<anonymous Java parameter>"),
                                    returnTypeRef = valueParameter.type.toFirResolvedTypeRef(),
                                    defaultValue = null, isCrossinline = false, isNoinline = false,
                                    isVararg = valueParameter.isVararg
                                )
                            }
                        }
                        firClass.declarations += memberFunction
                    }
                }
                for (declaration in firClass.declarations) {
                    if (declaration is FirNamedFunction) {
                        val methodId = CallableId(callableId.packageName, callableId.className, declaration.name)
                        if (methodId == callableId) {
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
                FirClassImpl(
                    session, null, firSymbol as FirClassSymbol, javaClass.name,
                    javaClass.visibility, javaClass.modality,
                    isExpect = false, isActual = false,
                    classKind = javaClass.classKind,
                    isInner = !javaClass.isStatic, isCompanion = false,
                    isData = false, isInline = false
                ).apply {
                    for (typeParameter in javaClass.typeParameters) {
                        typeParameters += createTypeParameterSymbol(typeParameter.name).fir
                    }
                    addAnnotationsFrom(javaClass)
                    for (supertype in javaClass.supertypes) {
                        superTypeRefs += supertype.toFirResolvedTypeRef()
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
}

