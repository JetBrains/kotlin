// DONT_TARGET_EXACT_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
// DIAGNOSTICS: -MISPLACED_TYPE_PARAMETER_CONSTRAINTS, -NOTHING_TO_INLINE


class MemberScope {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testIdenticalPresenceOfTailrecModifier() {}<!>
    <!NO_TAIL_CALLS_FOUND!>tailrec<!> <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testIdenticalPresenceOfTailrecModifier() {}<!>

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testDifferencesInTailrecModifierPresence() {}<!>
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testDifferencesInTailrecModifierPresence() {}<!>

    <!NO_TAIL_CALLS_FOUND!>tailrec<!> <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testDifferencesInTailrecModifierPresenceReverse() {}<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testDifferencesInTailrecModifierPresenceReverse() {}<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral, tailrec */
