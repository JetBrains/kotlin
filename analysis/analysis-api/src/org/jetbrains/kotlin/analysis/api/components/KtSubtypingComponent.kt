/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtType

public abstract class KtSubtypingComponent : KtAnalysisSessionComponent() {
    public abstract fun isEqualTo(first: KtType, second: KtType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean
    public abstract fun isSubTypeOf(subType: KtType, superType: KtType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean
}

public interface KtSubtypingComponentMixIn : KtAnalysisSessionMixIn {
    public fun KtType.isEqualTo(other: KtType): Boolean =
        withValidityAssertion { analysisSession.subtypingComponent.isEqualTo(this, other, KaSubtypingErrorTypePolicy.STRICT) }

    public fun KtType.isEqualTo(other: KtType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        withValidityAssertion { analysisSession.subtypingComponent.isEqualTo(this, other, errorTypePolicy) }

    public fun KtType.isSubTypeOf(superType: KtType): Boolean =
        withValidityAssertion { analysisSession.subtypingComponent.isSubTypeOf(this, superType, KaSubtypingErrorTypePolicy.STRICT) }

    public fun KtType.isSubTypeOf(superType: KtType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        withValidityAssertion { analysisSession.subtypingComponent.isSubTypeOf(this, superType, errorTypePolicy) }

    public fun KtType.isNotSubTypeOf(superType: KtType): Boolean =
        withValidityAssertion { !analysisSession.subtypingComponent.isSubTypeOf(this, superType, KaSubtypingErrorTypePolicy.STRICT) }

    public fun KtType.isNotSubTypeOf(superType: KtType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        withValidityAssertion { !analysisSession.subtypingComponent.isSubTypeOf(this, superType, errorTypePolicy) }
}

/**
 * [KaSubtypingErrorTypePolicy] determines the treatment of error types in type equality and subtyping checks.
 */
public enum class KaSubtypingErrorTypePolicy {
    /**
     * Error types are not considered equal to or subtypes of any other type.
     *
     * [STRICT] is the default policy for the following reasons:
     *
     *  1. In general, considering an error type equal to all other types is unintuitive. It essentially turns error types into dynamic `Any`
     *     or `Nothing` types (depending on the typing position). This can produce a lot of typing relationships which do not make any sense,
     *     such as `Int = UnresolvedClass`.
     *  2. It forces the user to handle error types explicitly, which reduces the risk of false positives.
     *  3. It is consistent with most of the behavior of the Kotlin compiler.
     */
    STRICT,

    /**
     * Error types are equal to and subtypes of all types.
     *
     * [LENIENT] should be chosen if type errors are expected and should be treated as type holes that can be satisfied by any other type.
     * However, caution should be applied when using [LENIENT], as the policy can swallow type errors which should have been dealt with
     * explicitly. In most cases, explicit type error handling should be preferred.
     *
     * As a motivating example, consider that we want to find all functions which can be called with an argument of a certain type and show
     * them to a user. The user has provided a type `List<>` with an unspecified type argument, which translates to `List<ERROR>`. We still
     * want to show functions like `foo(list: List<Int>)`, as the user hasn't filled out the type argument yet. With the [LENIENT] policy,
     * `List<ERROR>` is a subtype of `List<Int>`, meaning that `foo(list: List<Int>)` is a valid candidate for our purposes.
     */
    LENIENT,
}
