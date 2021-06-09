/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.types

import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgument
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

sealed interface KtType : ValidityTokenOwner {
    fun asStringForDebugging(): String
}

interface KtTypeWithNullability : KtType {
    val nullability: KtTypeNullability
}

enum class KtTypeNullability(val isNullable: Boolean) {
    NULLABLE(true), NON_NULLABLE(false);

    companion object {
        fun create(isNullable: Boolean) = if (isNullable) NULLABLE else NON_NULLABLE
    }
}

sealed class KtClassType : KtType {
    override fun toString(): String = asStringForDebugging()
}

sealed class KtNonErrorClassType : KtClassType(), KtTypeWithNullability {
    abstract val classId: ClassId
    abstract val classSymbol: KtClassLikeSymbol
    abstract val typeArguments: List<KtTypeArgument>
}

abstract class KtFunctionalType : KtNonErrorClassType() {
    abstract val isSuspend: Boolean
    abstract val arity: Int
    abstract val receiverType: KtType?
    abstract val hasReceiver: Boolean
    abstract val parameterTypes: List<KtType>
    abstract val returnType: KtType
}

abstract class KtUsualClassType : KtNonErrorClassType()

abstract class KtClassErrorType : KtClassType() {
    abstract val error: String
}

abstract class KtTypeParameterType : KtTypeWithNullability {
    abstract val name: Name
    abstract val symbol: KtTypeParameterSymbol
}

abstract class KtCapturedType : KtType {
    override fun toString(): String = asStringForDebugging()
}

abstract class KtDefinitelyNotNullType : KtType, KtTypeWithNullability {
    abstract val original: KtType

    final override val nullability: KtTypeNullability get() = KtTypeNullability.NON_NULLABLE

    override fun toString(): String = asStringForDebugging()
}

abstract class KtFlexibleType : KtType {
    abstract val lowerBound: KtType
    abstract val upperBound: KtType

    override fun toString(): String = asStringForDebugging()
}

abstract class KtIntersectionType : KtType {
    abstract val conjuncts: List<KtType>

    override fun toString(): String = asStringForDebugging()
}