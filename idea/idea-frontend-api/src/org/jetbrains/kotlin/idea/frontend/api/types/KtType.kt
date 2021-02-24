/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.types

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgument
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

interface KtType : ValidityTokenOwner {
    fun asStringForDebugging(): String
}

interface KtTypeWithNullability : KtType {
    val nullability: KtTypeNullability
}

enum class KtTypeNullability {
    NULLABLE, NON_NULLABLE;

    companion object {
        fun create(isNullable: Boolean) = if (isNullable) NULLABLE else NON_NULLABLE
    }
}

sealed class KtDenotableType : KtType {
    abstract fun asString(): String
}

sealed class KtClassType : KtDenotableType(), KtTypeWithNullability {
    abstract val classId: ClassId
    abstract val classSymbol: KtClassLikeSymbol
    abstract val typeArguments: List<KtTypeArgument>
}

abstract class KtFunctionalType : KtClassType() {
    abstract val isSuspend: Boolean
    abstract val arity: Int
    abstract val receiverType: KtType?
    abstract val parameterTypes: List<KtType>
    abstract val returnType: KtType
}

abstract class KtUsualClassType : KtClassType()

abstract class KtErrorType : KtType {
    abstract val error: String
}

abstract class KtTypeParameterType : KtDenotableType(), KtTypeWithNullability {
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