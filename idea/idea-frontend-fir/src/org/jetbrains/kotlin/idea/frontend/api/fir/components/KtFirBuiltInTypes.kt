/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtBuiltinTypes
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirUsualClassType
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.ValidityAwareCachedValue
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.types.KtType

internal class KtFirBuiltInTypes(builtinTypes: BuiltinTypes, builder: KtSymbolByFirBuilder, override val token: ValidityToken) : KtBuiltinTypes() {
    private val builder by weakRef(builder)

    override val INT: KtType by cachedBuiltin(builtinTypes.intType)
    override val LONG: KtType by cachedBuiltin(builtinTypes.longType)
    override val SHORT: KtType by cachedBuiltin(builtinTypes.shortType)
    override val BYTE: KtType by cachedBuiltin(builtinTypes.byteType)

    override val FLOAT: KtType by cachedBuiltin(builtinTypes.floatType)
    override val DOUBLE: KtType by cachedBuiltin(builtinTypes.doubleType)

    override val CHAR: KtType by cachedBuiltin(builtinTypes.charType)
    override val BOOLEAN: KtType by cachedBuiltin(builtinTypes.booleanType)
    override val STRING: KtType by cachedBuiltin(builtinTypes.stringType)

    override val UNIT: KtType by cachedBuiltin(builtinTypes.unitType)
    override val NOTHING: KtType by cachedBuiltin(builtinTypes.nothingType)
    override val ANY: KtType by cachedBuiltin(builtinTypes.anyType)


    override val NULLABLE_ANY: KtType by cachedBuiltin(builtinTypes.nullableAnyType)
    override val NULLABLE_NOTHING: KtType by cachedBuiltin(builtinTypes.nullableNothingType)

    private fun cachedBuiltin(builtinTypeRef: FirImplicitBuiltinTypeRef): ValidityAwareCachedValue<KtFirUsualClassType> = cached {
        KtFirUsualClassType(builtinTypeRef.type as ConeClassLikeTypeImpl, token, builder) // TODO builder leaking
    }
}