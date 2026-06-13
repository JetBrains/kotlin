// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-30036

// KT-30036: Different inference results for postponed arguments with expected type for old and new inference

fun <T> id(x: T): T = x

fun test() {
    val a: (String) -> Int = id { 42 } // Error in old inference, ok in the new one
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, typeParameter */
