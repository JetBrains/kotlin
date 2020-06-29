/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.types

import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgument
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

interface KtType : ValidityOwner {
    fun isEqualTo(other: KtType): Boolean
    fun isSubTypeOf(superType: KtType): Boolean

    fun asStringForDebugging(): String
}

sealed class KtDenotableType : KtType {
    abstract fun asString(): String
}

abstract class KtClassType : KtDenotableType() {
    abstract val classId: ClassId
    abstract val classSymbol: KtClassLikeSymbol
    abstract val typeArguments: List<KtTypeArgument>
}

abstract class KtErrorType : KtType {
    abstract val error: String
}

abstract class KtTypeParameterType : KtDenotableType() {
    abstract val name: Name
    abstract val symbol: KtTypeParameterSymbol
}

sealed class KtNonDenotableType : KtType

abstract class KtFlexibleType : KtNonDenotableType() {
    abstract val lowerBound: KtType
    abstract val upperBound: KtType
}

abstract class KtIntersectionType : KtNonDenotableType() {
    abstract val conjuncts: List<KtType>
}