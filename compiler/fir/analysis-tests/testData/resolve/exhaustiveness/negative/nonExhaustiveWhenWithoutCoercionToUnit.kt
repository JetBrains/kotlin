// RUN_PIPELINE_TILL: BACKEND
fun <T> run(block: () -> T): T = block()

fun test(a: Any) {
    run {
        // Should be an error, see KT-44810
        when (a) {
            is String -> 1
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, isExpression, lambdaLiteral, nullableType,
typeParameter, whenExpression, whenWithSubject */
