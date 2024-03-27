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

    /**
     * The abbreviated type for this expanded [KtType], or `null` if this type has not been expanded from an abbreviated type or the
     * abbreviated type cannot be resolved.
     *
     * An abbreviated type is a type alias application that has been expanded to some other Kotlin type. For example, if we have a type
     * alias `typealias MyString = String` and its application `MyString`, `String` would be the type alias expansion and `MyString` its
     * abbreviated type.
     *
     * The abbreviated type contains the type arguments of a specific type alias application. For example, if we have a
     * `typealias MyList<A> = List<A>`, for an application `MyList<String>`, `MyList<String>` would be the abbreviated type for such a type
     * alias application, not simply `MyList`.
     *
     * If this [KtType] is an unexpanded type alias application, [abbreviatedType] is `null`. Not all type alias applications are currently
     * expanded right away and the Analysis API makes no guarantees about the specific circumstances.
     *
     * While [abbreviatedType] is available for all [KtType]s, it can currently only be present in [KtClassType]s. However, abbreviated
     * types are a general concept and if the type system changes (e.g. with denotable union/intersection types), other kinds of types may
     * also be expanded from a type alias. This would allow more kinds of types to carry an abbreviated type.
     *
     * The [abbreviatedType] itself is always a [KtUsualClassType], as the application of a type alias is always a class type. It cannot be
     * a [KtClassErrorType] because [abbreviatedType] would then be `null`.
     *
     *
     * ### Resolvability
     *
     * Even when this [KtType] is an expansion, the abbreviated type may be `null` if it is not resolvable from this type's use-site module.
     * This can occur when the abbreviated type from a module `M1` was expanded at some declaration `D` in module `M2`, and the use-site
     * module uses `D`, but only has a dependency on `M2`. Then the type alias of `M1` remains unresolved and [abbreviatedType] is `null`.
     *
     *
     * ### Type arguments and nested abbreviated types
     *
     * The type arguments of an abbreviated type are not converted to abbreviated types automatically. That is, if a type argument is a type
     * expansion, its [abbreviatedType] doesn't automatically replace the expanded type. For example:
     *
     * ```
     * typealias MyString = String
     * typealias MyList<A> = List<A>
     *
     * val list: MyList<MyString> = listOf()
     * ```
     *
     * `MyList<MyString>` may be expanded to a type `List<String>` with an abbreviated type `MyList<String>`, where `String` also has the
     * abbreviated type `MyString`. The abbreviated type is not `MyList<MyString>`, although it might be rendered as such.
     *
     *
     * ### Transitive expansion
     *
     * Types are always expanded to their final form. That is, if we have a chain of type alias expansions, the [KtType] only represents the
     * final expanded type, and its [abbreviatedType] the initial type alias application. For example:
     *
     * ```
     * typealias Inner = String
     * typealias Outer = Inner
     *
     * val outer: Outer = ""
     * ```
     *
     * Here, `outer`'s type would be expanded to `String`, but its abbreviated type would be `Outer`. `Inner` would be lost.
     */
    public val abbreviatedType: KtUsualClassType?

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
    public abstract val isReflectType: Boolean
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

/**
 * A flexible type's [abbreviatedType] is always `null`, as only [lowerBound] and [upperBound] may actually be expanded types.
 */
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
