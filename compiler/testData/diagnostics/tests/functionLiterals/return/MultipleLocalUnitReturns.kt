// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun <T> execute(block: () -> T): T = block()

fun main() {
    execute {
        if (true) return@execute
        if (true) return@execute Unit
        execute { Any() }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, lambdaLiteral, nullableType, typeParameter */
