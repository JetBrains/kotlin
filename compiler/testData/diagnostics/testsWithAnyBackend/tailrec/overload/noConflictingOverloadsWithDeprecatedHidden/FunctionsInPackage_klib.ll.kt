// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests do not run backends, so backend-only diagnostics are not reported.
// LL_FIR_DIVERGENCE
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING

package pkg

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testIdenticalPresenceOfTailrecModifier() {}
<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testIdenticalPresenceOfTailrecModifier() {}

@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testDifferencesInTailrecModifierPresence() {}
fun testDifferencesInTailrecModifierPresence() {}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testDifferencesInTailrecModifierPresenceReverse() {}
@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInTailrecModifierPresenceReverse() {}


/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral, tailrec */
