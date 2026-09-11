// DONT_TARGET_EXACT_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testIdenticalPresenceOfTailrecModifier() {}<!>
<!CONFLICTING_KLIB_SIGNATURES_ERROR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testIdenticalPresenceOfTailrecModifier() {}<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testDifferencesInTailrecModifierPresence() {}<!>
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun testDifferencesInTailrecModifierPresence() {}<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testDifferencesInTailrecModifierPresenceReverse() {}<!>
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInTailrecModifierPresenceReverse() {}<!>


/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral, tailrec */
