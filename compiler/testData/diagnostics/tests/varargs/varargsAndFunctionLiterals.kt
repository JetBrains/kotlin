// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun f(vararg t : Int, f : ()->Unit) {
}

fun test() {
    f {}
    f() {}
    f(1) {

    }
    f(1, 2) {

    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, vararg */
