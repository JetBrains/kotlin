/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.utils.sure

/**
 * In case of MemberScopeTowerLevel with smart cast dispatch receiver, we may create candidates both from smart cast type and from
 * the member scope of original expression's type (without smart cast).
 *
 * It might be necessary because the ones from smart cast might be invisible (e.g., because they are protected in other class).
 *
 * open class A {
 *   open protected fun foo(a: Derived) {}
 *   fun f(a: A, d: Derived) {
 *      when (a) {
 *          is B -> {
 *              a.foo(d) // should be resolved to A::foo, not the public B::foo
 *          }
 *      }
 *   }
 * }
 * class B : A() {
 *  override fun foo(a: Derived) {}
 *  public fun foo(a: Base) {}
 * }
 *
 * If we would just resolve `a.foo(d)` if `a` had a type B, then we would choose a public B::foo, because the other one `foo` is protected in B,
 * so we can't call it outside the B subclasses.
 *
 * But that resolution result would be less precise result that the one before smart-cast applied (A::foo has more specific parameters),
 * so at MemberScopeTowerLevel we create candidates both from A's and B's scopes on the same level.
 *
 * But in case when there would be successful candidates from both types, we discriminate ones from original type, thus sticking to the candidates
 * from smart cast type.
 *
 * See more details at KT-51460, KT-55722, KT-56310 and relevant tests
 * - testData/diagnostics/tests/visibility/moreSpecificProtectedSimple.kt
 * - testData/diagnostics/tests/smartCasts/kt51460.kt
 */
object FilteringOutOriginalInPresenceOfSmartCastConeCallConflictResolver : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        val (originalIfSmartCastPresent, other) = candidates.partition { it.isFromOriginalTypeInPresenceOfSmartCast }

        // If we have both successful candidates from smart cast and original, use the former one as they might have more correct return type
        if (originalIfSmartCastPresent.isNotEmpty() && other.isNotEmpty()) return other.toSet().discriminateByInvokeVariablePriority()

        return candidates.discriminateByInvokeVariablePriority()
    }

    // See the relevant test at testData/diagnostics/tests/resolve/invoke/kt9517.kt
    private fun Set<Candidate>.discriminateByInvokeVariablePriority(): Set<Candidate> {
        if (size <= 1) return this

        // Resulting successful candidates should always belong to the same tower group.
        // Thus, if one of them is not variable + invoke, it should be applied to others, too.
        if (first().callInfo.candidateForCommonInvokeReceiver == null) return this

        val (originalIfSmartCastPresent, other) = partition {
            it.callInfo.candidateForCommonInvokeReceiver.sure {
                "If one candidate within a group is variable+invoke, other should be the same, but $it found"
            }.isFromOriginalTypeInPresenceOfSmartCast
        }

        if (originalIfSmartCastPresent.isNotEmpty() && other.isNotEmpty()) return other.toSet()

        return this
    }
}
