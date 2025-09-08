// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

fun main() {
  throw <!TYPE_MISMATCH!>"str"<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
