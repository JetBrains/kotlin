/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType

abstract class KtTypeProvider : KtAnalysisSessionComponent() {
    //TODO get rid of it
    abstract fun isBuiltinFunctionalType(type: KtType): Boolean

    abstract val builtinTypes: KtBuiltinTypes

    abstract fun buildSelfClassType(symbol: KtClassOrObjectSymbol): KtType
}


@Suppress("PropertyName")
abstract class KtBuiltinTypes : ValidityTokenOwner {
    abstract val INT: KtType
    abstract val LONG: KtType
    abstract val SHORT: KtType
    abstract val BYTE: KtType

    abstract val FLOAT: KtType
    abstract val DOUBLE: KtType

    abstract val BOOLEAN: KtType
    abstract val CHAR: KtType
    abstract val STRING: KtType

    abstract val UNIT: KtType
    abstract val NOTHING: KtType
    abstract val ANY: KtType

    abstract val NULLABLE_ANY: KtType
    abstract val NULLABLE_NOTHING: KtType
}