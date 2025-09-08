// RUN_PIPELINE_TILL: BACKEND
fun <T> run(block: () -> T): T = block()

fun test(a: Any, b: Boolean) {
    run {
        if (b) return@run
        when (a) {
            is String -> 1
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, integerLiteral, isExpression, lambdaLiteral,
nullableType, typeParameter, whenExpression, whenWithSubject */
