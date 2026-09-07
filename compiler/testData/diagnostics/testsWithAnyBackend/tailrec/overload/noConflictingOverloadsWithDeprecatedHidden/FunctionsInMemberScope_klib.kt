// DONT_TARGET_EXACT_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
// DIAGNOSTICS: -MISPLACED_TYPE_PARAMETER_CONSTRAINTS, -NOTHING_TO_INLINE, -NO_TAIL_CALLS_FOUND


class MemberScope {
    <!NO_TAIL_CALLS_FOUND_IN_IR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testIdenticalPresenceOfTailrecModifier() {}<!><!>
    <!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testIdenticalPresenceOfTailrecModifier() {}<!><!>

    <!NO_TAIL_CALLS_FOUND_IN_IR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testDifferencesInTailrecModifierPresence() {}<!><!>
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testDifferencesInTailrecModifierPresence() {}<!>

    <!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testDifferencesInTailrecModifierPresenceReverse() {}<!><!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testDifferencesInTailrecModifierPresenceReverse() {}<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral, tailrec */
