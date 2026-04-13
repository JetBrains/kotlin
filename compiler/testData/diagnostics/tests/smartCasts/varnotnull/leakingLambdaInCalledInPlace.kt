// RUN_PIPELINE_TILL: FRONTEND
fun main() {
    var p: String?
    var block: () -> Int = { 1 }
    p = "2"
    run {
        block = { <!SMARTCAST_IMPOSSIBLE!>p<!>.length }
    }
    p = null
    block()
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, smartcast, stringLiteral */
