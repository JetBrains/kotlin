// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-37498

// KT-37498: Misleading diagnostic message for incorrectly specified lambda parameter

fun <R> foo(op: (R) -> R) {}
val test = foo <!ARGUMENT_TYPE_MISMATCH("(String) -> Unit; (String) -> String")!>{ _: String -> }<!>

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, propertyDeclaration,
typeParameter */
