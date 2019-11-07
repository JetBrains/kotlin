/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
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
        repeat(size - sizeBefore) {
            towerScope.scopes.let { it.removeAt(it.size - 1) }
        }
        return result
    }

    protected fun resolveNestedClassesSupertypes(
        regularClass: FirRegularClass,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return withScopeCleanup {
            // ? Is it Ok to use original file session here ?
            lookupSuperTypes(regularClass, lookupInterfaces = false, deep = true, useSiteSession = session)
                .asReversed().mapTo(towerScope.scopes) {
                    nestedClassifierScope(it.lookupTag.classId, session)
                }
            regularClass.addTypeParametersScope()
            val companionObject = regularClass.companionObject
            if (companionObject != null) {
                towerScope.scopes += nestedClassifierScope(companionObject)
            }
            towerScope.scopes += nestedClassifierScope(regularClass)

            transformElement(regularClass, data)
        }
    }

    protected fun FirMemberDeclaration.addTypeParametersScope() {
        val scopes = towerScope.scopes
        if (typeParameters.isNotEmpty()) {
            scopes += FirMemberTypeParameterScope(this)
        }
    }
}
