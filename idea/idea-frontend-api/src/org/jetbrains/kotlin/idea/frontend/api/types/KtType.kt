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

public sealed interface KtType : ValidityTokenOwner {
    public val nullability: KtTypeNullability
    public fun asStringForDebugging(): String
}

public enum class KtTypeNullability(public val isNullable: Boolean) {
    NULLABLE(true),
    NON_NULLABLE(false),
    UNKNOWN(false);

    public companion object {
        public fun create(isNullable: Boolean): KtTypeNullability = if (isNullable) NULLABLE else NON_NULLABLE
    }
}

public sealed class KtClassType : KtType {
    override fun toString(): String = asStringForDebugging()
}

public sealed class KtNonErrorClassType : KtClassType() {
    public abstract val classId: ClassId
    public abstract val classSymbol: KtClassLikeSymbol
    public abstract val typeArguments: List<KtTypeArgument>
}

public abstract class KtFunctionalType : KtNonErrorClassType() {
    public abstract val isSuspend: Boolean
    public abstract val arity: Int
    public abstract val receiverType: KtType?
    public abstract val hasReceiver: Boolean
    public abstract val parameterTypes: List<KtType>
    public abstract val returnType: KtType
}

public abstract class KtUsualClassType : KtNonErrorClassType()

public abstract class KtClassErrorType : KtClassType() {
    public abstract val error: String
    public abstract val candidateClassSymbols: Collection<KtClassLikeSymbol>
}

public abstract class KtTypeParameterType : KtType {
    public abstract val name: Name
    public abstract val symbol: KtTypeParameterSymbol
}

public abstract class KtCapturedType : KtType {
    override fun toString(): String = asStringForDebugging()
}

public abstract class KtDefinitelyNotNullType : KtType {
    public abstract val original: KtType

    final override val nullability: KtTypeNullability get() = KtTypeNullability.NON_NULLABLE

    override fun toString(): String = asStringForDebugging()
}

public abstract class KtFlexibleType : KtType {
    public abstract val lowerBound: KtType
    public abstract val upperBound: KtType

    override fun toString(): String = asStringForDebugging()
}

public abstract class KtIntersectionType : KtType {
    public abstract val conjuncts: List<KtType>

    override fun toString(): String = asStringForDebugging()
}