/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.signaturer

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * The name that an IR node generated from this [FirCallableDeclaration] will have, e.g. `<init>` for constructors,
 * `<get-foo>` and `<set-foo>` for property accessors, name as is for variables and simple functions,
 * and `<anonymous>` for anonymous functions.
 */
val FirCallableDeclaration.irName: Name
    get() = when (this) {
        is FirConstructor -> SpecialNames.INIT
        is FirSimpleFunction -> this.name
        is FirPropertyAccessor -> this.irName
        is FirVariable -> this.name
        is FirFunction -> SpecialNames.ANONYMOUS
    }

/**
 * The name that an IR node generated from this [FirPropertyAccessor] will have.
 *
 * If the corresponding property has name `foo`, this will return `<get-foo>` or `<set-foo>`.
 */
val FirPropertyAccessor.irName: Name
    get() {
        val prefix = when {
            isGetter -> "<get-"
            isSetter -> "<set-"
            else -> error("unknown property accessor kind $this")
        }
        return Name.special(prefix + propertySymbol.fir.name + ">")
    }
