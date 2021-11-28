/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

typealias AnnotationFqn = FqName

/*
 * Accessing extensions
 *
 * - specific extension -> all extension instances: extension accessor on `FirExtensionService`
 * - all declarations matching extension with predicate -> FirPredicateBasedProvider.getSymbolsByPredicate
 * - all specific extensions interested in specific declaration -> TODO with StateMachine
 */
abstract class FirExtension(val session: FirSession) {
    abstract val name: FirExtensionPointName

    abstract val key: FirPluginKey

    abstract val extensionType: KClass<out FirExtension>

    fun interface Factory<out P : FirExtension> {
        fun create(session: FirSession): P
    }
}

abstract class FirPredicateBasedExtension(session: FirSession) : FirExtension(session) {
    abstract fun FirDeclarationPredicateRegistrar.registerPredicates()
}

data class FirExtensionPointName(val name: Name) {
    constructor(name: String) : this(Name.identifier(name))
}

abstract class FirDeclarationPredicateRegistrar {
    abstract fun register(vararg predicates: DeclarationPredicate)
    abstract fun register(predicates: Collection<DeclarationPredicate>)
}

@RequiresOptIn
annotation class FirExtensionApiInternals
