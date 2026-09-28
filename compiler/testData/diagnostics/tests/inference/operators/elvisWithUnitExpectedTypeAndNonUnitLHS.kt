// RUN_PIPELINE_TILL: CODEGEN

fun unitFun() {}

fun myRun(x: () -> Unit) {}

fun nullStr(): String? = null

fun baz() {
    myRun {
        nullStr() ?: unitFun()
    }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, functionalType, lambdaLiteral, nullableType */
