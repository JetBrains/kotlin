// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +LateinitLocalVariables

fun test1() {
    lateinit var s: String
    s.length
}

fun test2() {
    lateinit var s: String
    run {
        s = ""
    }
    s.length
}

fun almostAlwaysTrue(): Boolean = true

fun test3() {
    lateinit var s: String
    if (almostAlwaysTrue()) {
        s = ""
    }
    s.length
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, ifExpression, lambdaLiteral, lateinit, localProperty,
propertyDeclaration, stringLiteral */
