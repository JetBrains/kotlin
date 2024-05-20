/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaBuiltinTypes
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.ValidityAwareCachedValue
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef

internal class KaFirBuiltInTypes(
    builtinTypes: BuiltinTypes,
    private val builder: KaSymbolByFirBuilder,
    override val token: KaLifetimeToken
) : KaBuiltinTypes() {

    override val INT: KaType by cachedBuiltin(builtinTypes.intType)
    override val LONG: KaType by cachedBuiltin(builtinTypes.longType)
    override val SHORT: KaType by cachedBuiltin(builtinTypes.shortType)
    override val BYTE: KaType by cachedBuiltin(builtinTypes.byteType)

    override val FLOAT: KaType by cachedBuiltin(builtinTypes.floatType)
    override val DOUBLE: KaType by cachedBuiltin(builtinTypes.doubleType)

    override val CHAR: KaType by cachedBuiltin(builtinTypes.charType)
    override val BOOLEAN: KaType by cachedBuiltin(builtinTypes.booleanType)
    override val STRING: KaType by cachedBuiltin(builtinTypes.stringType)

    override val UNIT: KaType by cachedBuiltin(builtinTypes.unitType)
    override val NOTHING: KaType by cachedBuiltin(builtinTypes.nothingType)
    override val ANY: KaType by cachedBuiltin(builtinTypes.anyType)

    override val THROWABLE: KaType by cachedBuiltin(builtinTypes.throwableType)
    override val NULLABLE_ANY: KaType by cachedBuiltin(builtinTypes.nullableAnyType)
    override val NULLABLE_NOTHING: KaType by cachedBuiltin(builtinTypes.nullableNothingType)

    private fun cachedBuiltin(builtinTypeRef: FirImplicitBuiltinTypeRef): ValidityAwareCachedValue<KaType> = cached {
        builder.typeBuilder.buildKtType(builtinTypeRef)
    }
}