// RUN_PIPELINE_TILL: BACKEND
@Deprecated("No")
val f: () -> Unit = {}

fun test() {
    <!DEPRECATION!>f<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, propertyDeclaration, stringLiteral */
