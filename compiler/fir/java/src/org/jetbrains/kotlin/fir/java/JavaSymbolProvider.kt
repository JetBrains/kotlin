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
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
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
                type = ConeClassTypeImpl(FirClassSymbol(classId!!).toLookupTag(), emptyArray(), isNullable = false),
                isMarkedNullable = true,
                annotations = emptyList()
            )
        ).apply {
            for (argument in this@toFirAnnotationCall.arguments) {
                arguments += argument.toFirExpression()
            }
        }
    }

    // TODO: use kind here
    private fun <T> List<T>.createArrayOfCall(@Suppress("UNUSED_PARAMETER") kind: IrConstKind<T>): FirArrayOfCall {
        return FirArrayOfCallImpl(session, null).apply {
            for (element in this@createArrayOfCall) {
                arguments += element.createConstant()
            }
        }
    }

    private fun Any?.createConstant(): FirExpression {
        return when (this) {
            is Byte -> FirConstExpressionImpl(session, null, IrConstKind.Byte, this)
            is Short -> FirConstExpressionImpl(session, null, IrConstKind.Short, this)
            is Int -> FirConstExpressionImpl(session, null, IrConstKind.Int, this)
            is Long -> FirConstExpressionImpl(session, null, IrConstKind.Long, this)
            is Char -> FirConstExpressionImpl(session, null, IrConstKind.Char, this)
            is Float -> FirConstExpressionImpl(session, null, IrConstKind.Float, this)
            is Double -> FirConstExpressionImpl(session, null, IrConstKind.Double, this)
            is Boolean -> FirConstExpressionImpl(session, null, IrConstKind.Boolean, this)
            is String -> FirConstExpressionImpl(session, null, IrConstKind.String, this)
            null -> FirConstExpressionImpl(session, null, IrConstKind.Null, null)

            else -> FirErrorExpressionImpl(session, null, "Unknown value in JavaLiteralAnnotationArgument: $this")
        }
    }

    private fun JavaAnnotationArgument.toFirExpression(): FirExpression {
        // TODO: this.name
        return when (this) {
            is JavaLiteralAnnotationArgument -> {
                val value = value
                when (value) {
                    is ByteArray -> value.toList().createArrayOfCall(IrConstKind.Byte)
                    is ShortArray -> value.toList().createArrayOfCall(IrConstKind.Short)
                    is IntArray -> value.toList().createArrayOfCall(IrConstKind.Int)
                    is LongArray -> value.toList().createArrayOfCall(IrConstKind.Long)
                    is CharArray -> value.toList().createArrayOfCall(IrConstKind.Char)
                    is FloatArray -> value.toList().createArrayOfCall(IrConstKind.Float)
                    is DoubleArray -> value.toList().createArrayOfCall(IrConstKind.Double)
                    is BooleanArray -> value.toList().createArrayOfCall(IrConstKind.Boolean)
                    else -> value.createConstant()
                }
            }
            is JavaArrayAnnotationArgument -> FirArrayOfCallImpl(session, null).apply {
                for (element in getElements()) {
                    arguments += element.toFirExpression()
                }
            }
            is JavaEnumValueAnnotationArgument -> {
                FirFunctionCallImpl(session, null).apply {
                    val classId = this@toFirExpression.enumClassId
                    val entryName = this@toFirExpression.entryName
                    val calleeReference = if (classId != null && entryName != null) {
                        val callableSymbol = session.service<FirSymbolProvider>().getCallableSymbols(
                            CallableId(classId.packageFqName, classId.relativeClassName, entryName)
                        ).firstOrNull()
                        callableSymbol?.let {
                            FirResolvedCallableReferenceImpl(session, null, entryName, it)
                        }
                    } else {
                        null
                    }
                    this.calleeReference = calleeReference
                        ?: FirErrorNamedReference(session, null, "Strange Java enum value: ${this@toFirExpression}")
                }
            }
            is JavaClassObjectAnnotationArgument -> FirGetClassCallImpl(session, null).apply {
                val referencedType = getReferencedType()
                arguments += FirClassReferenceExpressionImpl(session, null, referencedType.toFirResolvedTypeRef())
            }
            is JavaAnnotationAsAnnotationArgument -> getAnnotation().toFirAnnotationCall()
            else -> FirErrorExpressionImpl(session, null, "Unknown JavaAnnotationArgument: ${this::class.java}")
        }
    }

    private fun flexibleType(create: (isNullable: Boolean) -> ConeKotlinType): ConeFlexibleType {
        return ConeFlexibleType(create(false), create(true))
    }

    private fun JavaClassifierType.toFirResolvedTypeRef(): FirResolvedTypeRef {
        val coneType = when (val classifier = classifier) {
            is JavaClass -> {
                val symbol = ConeClassLikeLookupTagImpl(classifier.classId!!)
                flexibleType { isNullable ->
                    ConeClassTypeImpl(symbol, typeArguments.map { it.toConeProjection() }.toTypedArray(), isNullable)
                }
            }
            is JavaTypeParameter -> {
                // TODO: it's unclear how to identify type parameter by the symbol
                // TODO: some type parameter cache (provider?)
                val symbol = createTypeParameterSymbol(classifier.name)
                flexibleType { isNullable -> ConeTypeParameterTypeImpl(symbol, isNullable) }
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

