/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

public sealed interface KaType : KaLifetimeOwner, KaAnnotated {
    public val nullability: KaTypeNullability

    /**
     * The abbreviated type for this expanded [KaType], or `null` if this type has not been expanded from an abbreviated type or the
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
     * If this [KaType] is an unexpanded type alias application, [abbreviatedType] is `null`. Not all type alias applications are currently
     * expanded right away and the Analysis API makes no guarantees about the specific circumstances.
     *
     * While [abbreviatedType] is available for all [KaType]s, it can currently only be present in [KaClassType]s. However, abbreviated
     * types are a general concept and if the type system changes (e.g. with denotable union/intersection types), other kinds of types may
     * also be expanded from a type alias. This would allow more kinds of types to carry an abbreviated type.
     *
     * The [abbreviatedType] itself is always a [KaUsualClassType], as the application of a type alias is always a class type. It cannot be
     * a [KaClassErrorType] because [abbreviatedType] would then be `null`.
     *
     *
     * ### Resolvability
     *
     * Even when this [KaType] is an expansion, the abbreviated type may be `null` if it is not resolvable from this type's use-site module.
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
     * Types are always expanded to their final form. That is, if we have a chain of type alias expansions, the [KaType] only represents the
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
    public val abbreviatedType: KaUsualClassType?

    @Deprecated("Use 'toString()' instead.", replaceWith = ReplaceWith("toString()"))
    public fun asStringForDebugging(): String {
        return withValidityAssertion { toString() }
    }
}

public typealias KtType = KaType

public enum class KaTypeNullability(public val isNullable: Boolean) {
    NULLABLE(true),
    NON_NULLABLE(false),
    UNKNOWN(false);

    public companion object {
        public fun create(isNullable: Boolean): KaTypeNullability = if (isNullable) NULLABLE else NON_NULLABLE
    }
}

public typealias KtTypeNullability = KaTypeNullability

public interface KaErrorType : KaType {
    @KaAnalysisNonPublicApi
    public val errorMessage: String

    @KaAnalysisNonPublicApi
    public val presentableText: String?

    @KaAnalysisNonPublicApi
    @Deprecated("Use 'presentableText' instead.")
    public fun tryRenderAsNonErrorType(): String? = presentableText
}

public typealias KtErrorType = KaErrorType

public typealias KaTypeErrorType = KaErrorType

public typealias KtTypeErrorType = KaTypeErrorType

public sealed class KaNonErrorClassType : KaType {
    public abstract val classId: ClassId
    public abstract val symbol: KaClassLikeSymbol
    public abstract val typeArguments: List<KaTypeProjection>

    public abstract val qualifiers: List<KaClassTypeQualifier.KaResolvedClassTypeQualifier>

    @Deprecated("Use 'symbol' instead.", ReplaceWith("symbol"))
    public val classSymbol: KaClassLikeSymbol
        get() = symbol

    @Deprecated("Use 'typeArguments' instead.", ReplaceWith("typeArguments"))
    public val ownTypeArguments: List<KaTypeProjection>
        get() = typeArguments
}

public typealias KtNonErrorClassType = KaNonErrorClassType

public abstract class KaFunctionalType : KaNonErrorClassType(), KaContextReceiversOwner {
    public abstract val isSuspend: Boolean
    public abstract val isReflectType: Boolean
    public abstract val arity: Int
    public abstract val hasContextReceivers: Boolean
    public abstract val receiverType: KaType?
    public abstract val hasReceiver: Boolean
    public abstract val parameterTypes: List<KaType>
    public abstract val returnType: KaType
}

public typealias KtFunctionalType = KaFunctionalType

public abstract class KaUsualClassType : KaNonErrorClassType()

public typealias KtUsualClassType = KaUsualClassType

public abstract class KaClassErrorType : KaErrorType {
    public abstract val qualifiers: List<KaClassTypeQualifier>

    public abstract val candidateSymbols: Collection<KaClassLikeSymbol>

    @Deprecated("Use 'candidateSymbols' instead.", ReplaceWith("candidateSymbols"))
    public val candidateClassSymbols: Collection<KaClassLikeSymbol>
        get() = candidateSymbols
}

public typealias KtClassErrorType = KaClassErrorType

public abstract class KaTypeParameterType : KaType {
    public abstract val name: Name
    public abstract val symbol: KaTypeParameterSymbol
}

public typealias KtTypeParameterType = KaTypeParameterType

public abstract class KaCapturedType : KaType {
    public abstract val projection: KaTypeProjection
}

public typealias KtCapturedType = KaCapturedType

public abstract class KaDefinitelyNotNullType : KaType {
    public abstract val original: KaType

    final override val nullability: KaTypeNullability get() = withValidityAssertion { KaTypeNullability.NON_NULLABLE }
}

public typealias KtDefinitelyNotNullType = KaDefinitelyNotNullType

/**
 * A flexible type's [abbreviatedType] is always `null`, as only [lowerBound] and [upperBound] may actually be expanded types.
 */
public abstract class KaFlexibleType : KaType {
    public abstract val lowerBound: KaType
    public abstract val upperBound: KaType
}

public typealias KtFlexibleType = KaFlexibleType

public abstract class KaIntersectionType : KaType {
    public abstract val conjuncts: List<KaType>
}

public typealias KtIntersectionType = KaIntersectionType

/**
 * A special dynamic type, which is used to support interoperability with dynamically typed libraries, platforms or languages.
 *
 * Although this can be viewed as a flexible type (kotlin.Nothing..kotlin.Any?), a platform may assign special meaning to the
 * values of dynamic type, and handle differently from the regular flexible type.
 */
public abstract class KaDynamicType : KaType

public typealias KtDynamicType = KaDynamicType