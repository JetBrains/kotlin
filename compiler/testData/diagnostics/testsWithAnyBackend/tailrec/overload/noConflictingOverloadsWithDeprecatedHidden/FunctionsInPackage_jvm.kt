// TARGET_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// IGNORE_DEXING

package pkg

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testIdenticalPresenceOfTailrecModifier()<!> {}
<!CONFLICTING_JVM_DECLARATIONS!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testIdenticalPresenceOfTailrecModifier()<!> {}

<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testDifferencesInTailrecModifierPresence()<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun testDifferencesInTailrecModifierPresence()<!> {}

<!CONFLICTING_JVM_DECLARATIONS!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun testDifferencesInTailrecModifierPresenceReverse()<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN) fun testDifferencesInTailrecModifierPresenceReverse()<!> {}


/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral, tailrec */
