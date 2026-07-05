// TARGET_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING
// DIAGNOSTICS: -MISPLACED_TYPE_PARAMETER_CONSTRAINTS, -NOTHING_TO_INLINE, -NO_TAIL_CALLS_FOUND


class MemberScope {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalPresenceOfTailrecModifier() {}<!>
    tailrec <!CONFLICTING_JVM_DECLARATIONS!>fun testIdenticalPresenceOfTailrecModifier() {}<!>

    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) tailrec <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresence() {}<!>
    <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresence() {}<!>

    tailrec <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresenceReverse() {}<!>
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresenceReverse() {}<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral, tailrec */
