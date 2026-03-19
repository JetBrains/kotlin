// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-54290
// WITH_STDLIB

// KT-54290: There is no mutability check in an initializer of top-level vals

var changeX: () -> Unit = {}

val x: String = "Hello there".also {
    changeX = { <!VAL_REASSIGNMENT, VAL_REASSIGNMENT!>x<!> = "111" }
}

fun main() {
    println(x)
    changeX()
    println(x)
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, lambdaLiteral, propertyDeclaration,
stringLiteral */
