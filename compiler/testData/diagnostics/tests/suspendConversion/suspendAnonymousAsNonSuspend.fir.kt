// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-58055

fun <T> produce(arg: () -> T): T = arg()

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE!>produce<!> {
        <!RETURN_TYPE_MISMATCH!><!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {}<!> // CCE
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, lambdaLiteral, nullableType,
typeParameter */
