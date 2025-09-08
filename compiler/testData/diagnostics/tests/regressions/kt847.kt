// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION
fun <T> T.mustBe(t : T) {
    "$this must be$<!SYNTAX!>as<!>$t"
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, nullableType, stringLiteral, thisExpression,
typeParameter */
