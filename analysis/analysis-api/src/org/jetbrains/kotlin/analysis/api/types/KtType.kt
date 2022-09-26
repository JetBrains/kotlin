/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

public sealed interface KtType : KtLifetimeOwner, KtAnnotated {
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

public sealed interface KtErrorType : KtType {
    // todo should be replaced with diagnostics
    public val errorMessage: String
}

public abstract class KtTypeErrorType : KtErrorType {
    public abstract fun tryRenderAsNonErrorType(): String?
}

public sealed class KtClassType : KtType {
    override fun toString(): String = asStringForDebugging()

    public abstract val qualifiers: List<KtClassTypeQualifier>
}

public sealed class KtNonErrorClassType : KtClassType() {
    public abstract val classId: ClassId
    public abstract val classSymbol: KtClassLikeSymbol
    public abstract val ownTypeArguments: List<KtTypeProjection>

    abstract override val qualifiers: List<KtClassTypeQualifier.KtResolvedClassTypeQualifier>
}

public abstract class KtFunctionalType : KtNonErrorClassType(), KtContextReceiversOwner {
    public abstract val isSuspend: Boolean
    public abstract val arity: Int
    public abstract val hasContextReceivers: Boolean
    public abstract val receiverType: KtType?
    public abstract val hasReceiver: Boolean
    public abstract val parameterTypes: List<KtType>
    public abstract val returnType: KtType
}

public abstract class KtUsualClassType : KtNonErrorClassType()

public abstract class KtClassErrorType : KtClassType(), KtErrorType {
    public abstract val candidateClassSymbols: Collection<KtClassLikeSymbol>
}

public abstract class KtTypeParameterType : KtType {
    public abstract val name: Name
    public abstract val symbol: KtTypeParameterSymbol
}

public abstract class KtCapturedType : KtType {
    public abstract val projection: KtTypeProjection
    override fun toString(): String = asStringForDebugging()
}

public abstract class KtDefinitelyNotNullType : KtType {
    public abstract val original: KtType

    final override val nullability: KtTypeNullability get() = withValidityAssertion { KtTypeNullability.NON_NULLABLE }

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

/**
 * Non-denotable type representing some number type. This type generally come when retrieving some integer literal `KtType`
 * It is unknown which number type it exactly is, but possible options based on [value] can be retrieved via [possibleTypes].
 */
public abstract class KtIntegerLiteralType : KtType {
    /**
     * Literal value for which the type was created.
     */
    public abstract val value: Long

    /**
     * Is the type unsigned (i.e. corresponding literal had `u` suffix)
     */
    public abstract val isUnsigned: Boolean

    /**
     * The list of `Number` types the type may be represented as.
     *
     * The possible options are: `Byte`, `Short` ,`Int`, `Long`, `UByte`, `UShort` `UInt`, `ULong`
     */
    public abstract val possibleTypes: Collection<KtClassType>

    override fun toString(): String = asStringForDebugging()
}

/**
 * A special dynamic type, which is used to support interoperability with dynamically typed libraries, platforms or languages.
 *
 * Although this can be viewed as a flexible type (kotlin.Nothing..kotlin.Any?), a platform may assign special meaning to the
 * values of dynamic type, and handle differently from the regular flexible type.
 */
public abstract class KtDynamicType : KtType {
    override fun toString(): String = asStringForDebugging()
}
