/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.name.Name

/**
 * [FirNonStaticMembersScope] includes non-static callables and inner classes.
 *
 * Inner classes are included because they are accessible from a value of the outer class. For example:
 *
 * ```
 * class Outer {
 *     inner class Inner
 * }
 *
 * fun foo() {
 *     val outer = Outer()
 *     outer.Inner()
 * }
 * ```
 *
 * While Kotlin always expects a constructor call when accessing `outer.Inner`, it nonetheless requires inner classes to be contained in
 * non-static scopes.
 */
internal class FirNonStaticMembersScope(
    private val delegate: FirContainingNamesAwareScope,
) : FirCallableFilteringScope(delegate) {
    override fun isTargetCallable(callable: FirCallableSymbol<*>): Boolean = !callable.fir.isStatic

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }

    override fun getClassifierNames(): Set<Name> = delegate.getClassifierNames()

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        delegate.processInnerClassesByName(name, processor)
    }
}

internal fun FirScope.processInnerClassesByName(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
    processClassifiersByNameWithSubstitution(name) { classifier, substitutor ->
        val firDeclaration = classifier.fir
        if (firDeclaration is FirMemberDeclaration && firDeclaration.isInner) {
            processor(classifier, substitutor)
        }
    }
}
