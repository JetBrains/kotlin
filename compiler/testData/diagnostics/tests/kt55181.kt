// RUN_PIPELINE_TILL: FRONTEND

fun main() {
  throw <!TYPE_MISMATCH!>"str"<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
