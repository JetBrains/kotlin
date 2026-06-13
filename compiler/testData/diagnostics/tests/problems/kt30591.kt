// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-30591

// KT-30591: UNIT_EXPECTED_TYPE semantics with nested run lambdas and generic materialize
fun <T> materialize(): T = TODO()

fun c(): Unit = run {
    run {
        materialize()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, nullableType, typeParameter */
