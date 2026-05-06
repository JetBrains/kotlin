// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG

fun foo(): Int = 1

fun test(x: Any, y: String = x as String, z: Int = run { foo() }) {
    foo()
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, integerLiteral, lambdaLiteral */
