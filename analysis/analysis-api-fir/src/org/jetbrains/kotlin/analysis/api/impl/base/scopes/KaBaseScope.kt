/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.Name

/**
 * A base class for [KaScope] implementations providing the default behavior of some [KaScope] members. The remaining members
 * ([callables], [classifiers], [constructors], and so on) have to be implemented by the concrete scope.
 */
@KaImplementationDetail
abstract class KaBaseScope : KaScope {
    override val declarations: Sequence<KaDeclarationSymbol>
        get() = withValidityAssertion {
            sequence {
                yieldAll(callables)
                yieldAll(classifiers)
                yieldAll(constructors)
            }
        }

    override fun declarations(nameFilter: (Name) -> Boolean): Sequence<KaDeclarationSymbol> = withValidityAssertion {
        sequence {
            yieldAll(callables(nameFilter))
            yieldAll(classifiers(nameFilter))
        }
    }

    override fun declarations(names: Collection<Name>): Sequence<KaDeclarationSymbol> = withValidityAssertion {
        sequence {
            yieldAll(callables(names))
            yieldAll(classifiers(names))
        }
    }

    override fun declarations(vararg names: Name): Sequence<KaDeclarationSymbol> =
        declarations(names.toList())

    override val callables: Sequence<KaCallableSymbol>
        get() = callables { true }

    override fun callables(vararg names: Name): Sequence<KaCallableSymbol> =
        callables(names.toList())

    override val classifiers: Sequence<KaClassifierSymbol>
        get() = classifiers { true }

    override fun classifiers(vararg names: Name): Sequence<KaClassifierSymbol> =
        classifiers(names.toList())
}
