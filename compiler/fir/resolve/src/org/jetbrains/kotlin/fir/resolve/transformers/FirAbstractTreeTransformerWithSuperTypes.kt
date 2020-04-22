/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.getNestedClassifierScope
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult

abstract class FirAbstractTreeTransformerWithSuperTypes(
    phase: FirResolvePhase,
    reversedScopePriority: Boolean
) : FirAbstractTreeTransformer<Nothing?>(phase) {
    protected val towerScope = FirCompositeScope(mutableListOf(), reversedPriority = reversedScopePriority)

    protected inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val sizeBefore = towerScope.scopes.size
        val result = l()
        val size = towerScope.scopes.size
        assert(size >= sizeBefore)
        towerScope.dropLastScopes(size - sizeBefore)
        return result
    }

    protected fun resolveNestedClassesSupertypes(
        firClass: FirClass<*>,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return withScopeCleanup {
            // ? Is it Ok to use original file session here ?
            val superTypes = lookupSuperTypes(firClass, lookupInterfaces = false, deep = true, useSiteSession = session).asReversed()
            for (superType in superTypes) {
                session.getNestedClassifierScope(superType.lookupTag)?.let {
                    towerScope.addScope(it)
                }
            }
            if (firClass is FirRegularClass) {
                firClass.addTypeParametersScope()
                val companionObject = firClass.companionObject
                if (companionObject != null) {
                    towerScope.addScope(nestedClassifierScope(companionObject))
                }
            }

            towerScope.addScope(nestedClassifierScope(firClass))

            transformElement(firClass, data)
        }
    }

    protected fun FirMemberDeclaration.addTypeParametersScope() {
        if (typeParameters.isNotEmpty()) {
            towerScope.addScope(FirMemberTypeParameterScope(this))
        }
    }
}
