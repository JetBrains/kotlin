// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-27650
// WITH_STDLIB

// KT-27650: Literal suspend fun can be used as non-suspend lambda (ClassCastException at runtime)
fun f1(): () -> () -> Unit {
    return {
        <!RETURN_TYPE_MISMATCH!><!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {
            println(1)
        }<!>
    }
}

suspend fun main() {
    f1()()
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, integerLiteral, lambdaLiteral, suspend */
