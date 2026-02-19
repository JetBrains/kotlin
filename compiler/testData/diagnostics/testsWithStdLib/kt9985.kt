// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun foo(l: List<String>?) {
  Pair(l?.joinToString(), "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, safeCall, stringLiteral */
