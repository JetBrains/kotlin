// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
<!NOTHING_TO_INLINE!>inline<!> fun foo(vararg x: Any) {}

fun test(a: Any, b: Any, c: Any) {
    foo(a, { "" }, b)
}

/* GENERATED_FIR_TAGS: functionDeclaration, inline, lambdaLiteral, stringLiteral, vararg */
