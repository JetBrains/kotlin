// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun <T> myRun(runnable: () -> T): T = TODO()

interface Controller<F> {
    fun yield(t: F)
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun foo(x: List<String>) {
    val y = generate {
        myRun {
            yield("")
            Unit
        }
    }

    y.length
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, stringLiteral, suspend, typeParameter, typeWithExtension */
