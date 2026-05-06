// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-55689
// WITH_STDLIB

// KT-55689: False positive "NO_CAST_NEEDED"
interface Box<E> {
    fun <X> map(e: (E) -> X): Box<X>
}

fun <F> start(): Box<F> = TODO()

fun foo(): Box<Collection<String>> {
    return start<List<String>>().map { strings ->
        strings as Collection<String> // NO_CAST_NEEDED reported here, while removing it leads to red code
    }
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, typeParameter */
