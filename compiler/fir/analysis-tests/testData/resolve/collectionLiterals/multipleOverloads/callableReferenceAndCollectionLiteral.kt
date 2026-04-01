// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun <T> myForEach(set: Set<T>, action: (T) -> Unit) { }
fun <T> myForEach(set: Set<T>, action: (T) -> Long) { }

fun doSomething(x: String) { }
fun doSomething(x: Int) { }
fun doSomething(x: Char): Long = 0

fun useSites() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>myForEach<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>, ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>doSomething<!>)
    myForEach([42], ::doSomething)
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, integerLiteral, nullableType,
typeParameter */
