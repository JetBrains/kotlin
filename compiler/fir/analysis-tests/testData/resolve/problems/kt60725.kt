// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-60725
// WITH_STDLIB

// KT-60725: AssertionError during code generation when passing a non-suspend anonymous function as an argument with suspend type

fun <T> produce(arg: () -> (suspend () -> T)): T = TODO()

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE!>produce<!> { <!RETURN_TYPE_MISMATCH!>fun() {}<!> }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, lambdaLiteral, nullableType, suspend,
typeParameter */
