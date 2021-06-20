/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getNestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.wrapNestedClassifierScopeWithSubstitutionForSuperType
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType

abstract class FirAbstractTreeTransformerWithSuperTypes(
    phase: FirResolvePhase,
    protected val scopeSession: ScopeSession
) : FirAbstractTreeTransformer<Any?>(phase) {
    protected val scopes = mutableListOf<FirScope>()
    protected val towerScope = FirCompositeScope(scopes.asReversed())

    protected open fun needReplacePhase(firDeclaration: FirDeclaration<*>): Boolean = transformerPhase > firDeclaration.resolvePhase

    protected inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val sizeBefore = scopes.size
        val result = l()
        val size = scopes.size
        assert(size >= sizeBefore)
        repeat(size - sizeBefore) {
            scopes.removeAt(scopes.lastIndex)
        }
        return result
    }

    protected fun resolveNestedClassesSupertypes(
        firClass: FirClass<*>,
        data: Any?
    ): FirStatement {
        if (needReplacePhase(firClass)) {
            firClass.replaceResolvePhase(transformerPhase)
        }
        return withScopeCleanup {
            // Otherwise annotations may try to resolve
            // themselves as inner classes of the `firClass`
            // if their names match
            firClass.transformAnnotations(this, null)

            // ? Is it Ok to use original file session here ?
            val superTypes = lookupSuperTypes(
                firClass,
                lookupInterfaces = false,
                deep = true,
                substituteTypes = true,
                useSiteSession = session
            ).asReversed()
            for (superType in superTypes) {
                superType.lookupTag.getNestedClassifierScope(session, scopeSession)?.let { nestedClassifierScope ->
                    val scope = nestedClassifierScope.wrapNestedClassifierScopeWithSubstitutionForSuperType(superType, session)
                    scopes.add(scope)
                }
            }
            if (firClass is FirRegularClass) {
                firClass.addTypeParametersScope()
                val companionObject = firClass.companionObject
                if (companionObject != null) {
                    session.nestedClassifierScope(companionObject)?.let(scopes::add)
                }
            }

            session.nestedClassifierScope(firClass)?.let(scopes::add)

            // Note that annotations are still visited here
            // again, although there's no need in it
            transformDeclarationContent(firClass, data) as FirClass<*>
        }
    }

    protected fun FirStatusOwner.addTypeParametersScope() {
        if (typeParameters.isNotEmpty()) {
            scopes.add(FirMemberTypeParameterScope(this))
        }
    }

    open fun transformDeclarationContent(declaration: FirDeclaration<*>, data: Any?): FirDeclaration<*> {
        return transformElement(declaration, data)
    }
}

fun createSubstitutionForSupertype(superType: ConeLookupTagBasedType, session: FirSession): ConeSubstitutor {
    val klass = superType.lookupTag.toSymbol(session)?.fir as? FirRegularClass ?: return ConeSubstitutor.Empty
    val arguments = superType.typeArguments.map {
        it as? ConeKotlinType ?: ConeClassErrorType(ConeSimpleDiagnostic("illegal projection usage", DiagnosticKind.IllegalProjectionUsage))
    }
    val mapping = klass.typeParameters.map { it.symbol }.zip(arguments).toMap()
    return ConeSubstitutorByMap(mapping, session)
}
