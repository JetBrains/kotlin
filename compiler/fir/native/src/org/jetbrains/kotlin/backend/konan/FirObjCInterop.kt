/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.scopeForClass
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.DFS

private val objCDirectClassId = ClassId.topLevel(objCDirectFqName)
private val objCMethodClassId = ClassId.topLevel(objCMethodFqName)
private val objCObjectClassId = ClassId.topLevel(objCObjectFqName)
private val objCFactoryClassId = ClassId.topLevel(objCFactoryFqName)
private val objCConstructorClassId = ClassId.topLevel(objCConstructorFqName)
private val externalObjCClassClassId = ClassId.topLevel(externalObjCClassFqName)

@OptIn(SymbolInternals::class)
internal fun FirFunction.getObjCMethodInfoFromOverriddenFunctions(session: FirSession, scopeSession: ScopeSession): ObjCMethodInfo? {
    decodeObjCMethodAnnotation(session)?.let {
        return it
    }
    // recursively find ObjCMethod annotation in getDirectOverriddenFunctions() (same as `overriddenDescriptors` in K1)
    return when (val symbol = this.symbol) {
        is FirNamedFunctionSymbol -> {
            val firClassSymbol = containingClassLookupTag()?.toSymbol(session) as FirClassSymbol<*>?
            firClassSymbol?.let {
                val unsubstitutedScope = it.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
                // call of `processFunctionsByName()` is needed only for necessary side-effect before `getDirectOverriddenFunctions` call
                unsubstitutedScope.processFunctionsByName(symbol.name) {}
                unsubstitutedScope.getDirectOverriddenFunctions(symbol).firstNotNullOfOrNull {
                    assert(it.fir != this) { "Function ${symbol.name}() is wrongly contained in its own getDirectOverriddenFunctions" }
                    it.fir.getObjCMethodInfoFromOverriddenFunctions(session, scopeSession)
                }
            }
        }
        else -> null
    }
}

/**
 * mimics ConstructorDescriptor.getObjCInitMethod()
 */
@OptIn(SymbolInternals::class)
private fun FirConstructor.getObjCInitMethod(session: FirSession, scopeSession: ScopeSession): FirFunction? {
    this.annotations.getAnnotationByClassId(objCConstructorClassId, session)?.let { annotation ->
        val initSelector: String = annotation.constStringArgument("initSelector")
        val classSymbol = containingClassLookupTag()?.toSymbol(session) as FirClassSymbol<*>
        val initSelectors = mutableListOf<FirFunction>()
        classSymbol.fir.scopeForClass(ConeSubstitutor.Empty, session, scopeSession, classSymbol.toLookupTag(), memberRequiredPhase = null)
                .processAllFunctions {
                    if (it.fir.decodeObjCMethodAnnotation(session)?.selector == initSelector)
                        initSelectors.add(it.fir)
                }
        return initSelectors.singleOrNull()
                ?: error("expected one init method for $classSymbol $initSelector, got ${initSelectors.size}")
    }
    return null
}

/**
 * mimics FunctionDescriptor.decodeObjCMethodAnnotation()
 */
internal fun FirFunction.decodeObjCMethodAnnotation(session: FirSession): ObjCMethodInfo? =
        annotations.getAnnotationByClassId(objCMethodClassId, session)?.let {
            ObjCMethodInfo(
                    selector = it.constStringArgument("selector"),
                    encoding = it.constStringArgument("encoding"),
                    isStret = it.constBooleanArgumentOrNull("isStret") ?: false,
                    directSymbol = annotations.getAnnotationByClassId(objCDirectClassId, session)?.constStringArgument("symbol"),
            )
        }


private fun FirAnnotation.constStringArgument(argumentName: String): String =
        constArgument(argumentName) as? String ?: error("Expected string constant value of argument '$argumentName' at annotation $this")

private fun FirAnnotation.constBooleanArgumentOrNull(argumentName: String): Boolean? =
        constArgument(argumentName) as Boolean?

private fun FirAnnotation.constArgument(argumentName: String) =
        (argumentMapping.mapping[Name.identifier(argumentName)] as? FirConstExpression<*>)?.value

internal fun FirFunction.hasObjCFactoryAnnotation(session: FirSession) = this.annotations.hasAnnotation(objCFactoryClassId, session)

internal fun FirFunction.hasObjCMethodAnnotation(session: FirSession) = this.annotations.hasAnnotation(objCMethodClassId, session)

/**
 * almost mimics FunctionDescriptor.isObjCClassMethod(), apart from `it.isObjCClass()` changed to `it.symbol.isObjCClass(session)` for simplicity
 */
internal fun FirFunction.isObjCClassMethod(session: FirSession) =
        getContainingClass(session).let { it is FirClass && it.symbol.isObjCClass(session) }

/**
 * mimics ConstructorDescriptor.isObjCConstructor()
 */
internal fun FirConstructor.isObjCConstructor(session: FirSession) =
        this.annotations.hasAnnotation(objCConstructorClassId, session)

/**
 * mimics IrClass.isObjCClass()
 */
private fun FirClassSymbol<*>.isObjCClass(session: FirSession) = classId.packageFqName != interopPackageName &&
        selfOrAnySuperClass(session) {
            it.classId == objCObjectClassId
        }

/**
 * almost mimics `IrClass.selfOrAnySuperClass()` apart from using of classsymbol instead of class itself, to use `classId.toSymbol()`
 */
private fun FirClassSymbol<*>.selfOrAnySuperClass(session: FirSession, pred: (FirClassSymbol<*>) -> Boolean): Boolean =
        DFS.ifAny(
                listOf(this),
                { current ->
                    current.resolvedSuperTypes.mapNotNull {
                        (it.classId?.toSymbol(session) as? FirClassLikeSymbol)?.fullyExpandedClass(session)
                    }
                },
                pred
        )

internal fun FirFunction.getInitMethodIfObjCConstructor(session: FirSession, scopeSession: ScopeSession): FirFunction? =
        if (this is FirConstructor && isObjCConstructor(session))
            getObjCInitMethod(session, scopeSession)
        else
            this

internal fun FirProperty.isExternalObjCClassProperty(session: FirSession) =
        (getContainingClassSymbol(session) as? FirClassSymbol)?.isExternalObjCClass(session) == true

internal fun FirClassSymbol<*>.isExternalObjCClass(session: FirSession): Boolean =
        isObjCClass(session) &&
                parentsWithSelf(session).filterIsInstance<FirClass>().any {
                    it.hasAnnotation(externalObjCClassClassId, session)
                }

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.parentsWithSelf(session: FirSession): Sequence<FirClassLikeDeclaration> {
    return generateSequence<FirClassLikeDeclaration>(fir) { it.getContainingDeclaration(session) }
}
