/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.impl

import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.types.BirAbstractSimpleType
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirTypeAbbreviation
import org.jetbrains.kotlin.bir.types.BirTypeArgument
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.types.KotlinType

// note: used once for lazy deserialization
abstract class BirDelegatedSimpleType(kotlinType: KotlinType? = null) : BirAbstractSimpleType(kotlinType) {
    protected abstract val delegate: BirSimpleType

    override val classifier: BirClassifierSymbol
        get() = delegate.classifier
    override val nullability: SimpleTypeNullability
        get() = delegate.nullability
    override val arguments: List<BirTypeArgument>
        get() = delegate.arguments
    override val abbreviation: BirTypeAbbreviation?
        get() = delegate.abbreviation
    override val annotations: List<BirConstructorCall>
        get() = delegate.annotations
}