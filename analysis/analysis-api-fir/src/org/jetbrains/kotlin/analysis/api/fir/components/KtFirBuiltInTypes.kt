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

    override val int: KaType by cachedBuiltin(builtinTypes.intType)
    override val long: KaType by cachedBuiltin(builtinTypes.longType)
    override val short: KaType by cachedBuiltin(builtinTypes.shortType)
    override val byte: KaType by cachedBuiltin(builtinTypes.byteType)

    override val float: KaType by cachedBuiltin(builtinTypes.floatType)
    override val double: KaType by cachedBuiltin(builtinTypes.doubleType)

    override val char: KaType by cachedBuiltin(builtinTypes.charType)
    override val boolean: KaType by cachedBuiltin(builtinTypes.booleanType)
    override val string: KaType by cachedBuiltin(builtinTypes.stringType)

    override val unit: KaType by cachedBuiltin(builtinTypes.unitType)
    override val nothing: KaType by cachedBuiltin(builtinTypes.nothingType)
    override val any: KaType by cachedBuiltin(builtinTypes.anyType)

    override val throwable: KaType by cachedBuiltin(builtinTypes.throwableType)
    override val nullableAny: KaType by cachedBuiltin(builtinTypes.nullableAnyType)
    override val nullableNothing: KaType by cachedBuiltin(builtinTypes.nullableNothingType)

    private fun cachedBuiltin(builtinTypeRef: FirImplicitBuiltinTypeRef): ValidityAwareCachedValue<KaType> = cached {
        builder.typeBuilder.buildKtType(builtinTypeRef)
    }
}