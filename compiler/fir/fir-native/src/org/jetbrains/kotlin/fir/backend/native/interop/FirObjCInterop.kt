/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.native.interop

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.native.interop.ObjCMethodInfo


fun FirFunctionSymbol<*>.getObjCMethodInfoFromOverriddenFunctions(session: FirSession, scopeSession: ScopeSession): ObjCMethodInfo? {
    decodeObjCMethodAnnotation(session)?.let {
        return it
    }
    // recursively find ObjCMethod annotation in getDirectOverriddenFunctions() (same as `overriddenDescriptors` in K1)
    return when (val symbol = this) {
        is FirNamedFunctionSymbol -> {
            val firClassSymbol = containingClassLookupTag()?.toSymbol(session) as FirClassSymbol<*>?
            firClassSymbol?.let {
                val unsubstitutedScope = it.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
                // call of `processFunctionsByName()` is needed only for necessary side-effect before `getDirectOverriddenFunctions` call
                unsubstitutedScope.processFunctionsByName(symbol.name) {}
                unsubstitutedScope.getDirectOverriddenFunctions(symbol).firstNotNullOfOrNull {
                    require(it != this) { "Function ${symbol.name}() is wrongly contained in its own getDirectOverriddenFunctions" }
                    it.getObjCMethodInfoFromOverriddenFunctions(session, scopeSession)
                }
            }
        }
        else -> null
    }
}

/**
 * mimics ConstructorDescriptor.getObjCInitMethod()
 */
fun FirConstructorSymbol.getObjCInitMethod(session: FirSession): FirFunctionSymbol<*>? {
    this.resolvedAnnotationsWithClassIds.getAnnotationByClassId(NativeStandardInteropNames.objCConstructorClassId, session)?.let { annotation ->
        val initSelector: String = annotation.constStringArgument("initSelector")
        val classSymbol = containingClassLookupTag()?.toSymbol(session) as FirClassSymbol<*>
        val initSelectors = mutableListOf<FirFunctionSymbol<*>>()
        session.declaredMemberScope(classSymbol, memberRequiredPhase = null)
            .processAllFunctions {
                if (it.decodeObjCMethodAnnotation(session)?.selector == initSelector)
                    initSelectors.add(it)
            }
        return initSelectors.singleOrNull()
            ?: error("expected one init method for $classSymbol $initSelector, got ${initSelectors.size}")
    }
    return null
}

/**
 * mimics FunctionDescriptor.decodeObjCMethodAnnotation()
 */
internal fun List<FirAnnotation>.decodeObjCMethodAnnotation(session: FirSession): ObjCMethodInfo? =
    getAnnotationByClassId(NativeStandardInteropNames.objCMethodClassId, session)?.let {
        ObjCMethodInfo(
            selector = it.constStringArgument("selector"),
            encoding = it.constStringArgument("encoding"),
            isStret = it.constBooleanArgumentOrNull("isStret") ?: false,
            directSymbol = getAnnotationByClassId(NativeStandardInteropNames.objCDirectClassId, session)?.constStringArgument("symbol"),
        )
    }

internal fun FirFunctionSymbol<*>.decodeObjCMethodAnnotation(session: FirSession): ObjCMethodInfo? =
    resolvedAnnotationsWithClassIds.decodeObjCMethodAnnotation(session)


private fun FirAnnotation.constStringArgument(argumentName: String): String =
        constArgument(argumentName) as? String ?: error("Expected string constant value of argument '$argumentName' at annotation $this")

private fun FirAnnotation.constBooleanArgumentOrNull(argumentName: String): Boolean? =
        constArgument(argumentName) as Boolean?

private fun FirAnnotation.constArgument(argumentName: String) =
        (argumentMapping.mapping[Name.identifier(argumentName)] as? FirLiteralExpression)?.value

internal fun FirFunction.hasObjCFactoryAnnotation(session: FirSession) = this.annotations.hasAnnotation(NativeStandardInteropNames.objCFactoryClassId, session)

internal fun FirFunction.hasObjCMethodAnnotation(session: FirSession) = this.annotations.hasAnnotation(NativeStandardInteropNames.objCMethodClassId, session)

/**
 * Almost mimics FunctionDescriptor.isObjCClassMethod(), apart from `it.isObjCClass()`
 * changed to `it.symbol.isObjCClass(session)` for simplicity.
 * The containing symbol is resolved using the declaration-site session.
 */
internal fun FirFunction.isObjCClassMethod(session: FirSession) =
        getContainingClass().let { it is FirClass && it.symbol.isObjCClass(session) }

/**
 * mimics ConstructorDescriptor.isObjCConstructor()
 */
internal fun FirConstructorSymbol.isObjCConstructor(session: FirSession) =
    this.resolvedAnnotationsWithClassIds.hasAnnotation(NativeStandardInteropNames.objCConstructorClassId, session)

/**
 * mimics IrClass.isObjCClass()
 */
fun FirClassSymbol<*>.isObjCClass(session: FirSession) = classId.packageFqName != NativeStandardInteropNames.cInteropPackage &&
        selfOrAnySuperClass(session) {
            it.classId == NativeStandardInteropNames.objCObjectClassId
        }

private fun FirClassSymbol<*>.selfOrAnySuperClass(session: FirSession, predicate: (ConeClassLikeLookupTag) -> Boolean): Boolean =
    predicate(toLookupTag()) ||
            lookupSuperTypes(listOf(this), lookupInterfaces = true, deep = true, session, substituteTypes = false)
                .any { predicate(it.lookupTag) }

internal fun FirFunctionSymbol<*>.getInitMethodIfObjCConstructor(session: FirSession): FirFunctionSymbol<*>? =
        if (this is FirConstructorSymbol && isObjCConstructor(session))
            getObjCInitMethod(session)
        else
            this

fun FirProperty.isExternalObjCClassProperty(session: FirSession): Boolean =
        containingClassLookupTag()?.toClassSymbol(session)?.isExternalObjCClass(session) == true

internal fun FirClassSymbol<*>.isExternalObjCClass(session: FirSession): Boolean =
        isObjCClass(session) &&
                parentsWithSelf(session).filterIsInstance<FirClassSymbol<*>>().any {
                    it.hasAnnotation(NativeStandardInteropNames.externalObjCClassClassId, session)
                }

fun FirClassSymbol<*>.parentsWithSelf(session: FirSession): Sequence<FirClassLikeSymbol<FirClassLikeDeclaration>> {
    return generateSequence<FirClassLikeSymbol<FirClassLikeDeclaration>>(this) { it.getContainingDeclaration(session) }
}

fun FirClassSymbol<*>.isKotlinObjCClass(session: FirSession): Boolean = isObjCClass(session) && !isExternalObjCClass(session)

fun FirTypeRef.isObjCObjectType(session: FirSession): Boolean {
    val symbol = firClassLike(session)?.symbol
    return symbol is FirClassSymbol && symbol.isObjCClass(session)
}
