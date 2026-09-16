// RUN_PIPELINE_TILL: CODEGEN
fun foo(l: List<String>?) {
  Pair(l?.joinToString(), "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, safeCall, stringLiteral */
