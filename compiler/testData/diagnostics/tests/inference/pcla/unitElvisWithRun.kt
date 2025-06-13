// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-72238

interface Box<F> {
    fun add(f: F)
}

fun <E> myBuilder(x: Box<E>.() -> Unit) {}

fun <T, R> T.myRun(block: T.() -> R): R = TODO()

fun String.foo(result: String?) {
    myBuilder {
        result ?: myRun { Unit }
        add("a")
    }
}

/* GENERATED_FIR_TAGS: elvisExpression, funWithExtensionReceiver, functionDeclaration, functionalType,
interfaceDeclaration, lambdaLiteral, nullableType, stringLiteral, typeParameter, typeWithExtension */
