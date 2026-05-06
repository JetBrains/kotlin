// RUN_PIPELINE_TILL: BACKEND
fun f(t: (v: Int) -> Unit) {
    1.run(t)
}

fun main() {
    f { <!UNUSED_ANONYMOUS_PARAMETER!>i<!> ->

    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral */
