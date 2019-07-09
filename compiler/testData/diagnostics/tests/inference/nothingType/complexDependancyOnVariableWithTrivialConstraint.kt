// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

// Validation test.
// Check that type variable is fixed to Data<Nothing> as it's used in input types for lambda

class Data<T>(val x: T) {
    fun dataMethod() {}
}

fun <K> bar(x: K, y: (K) -> Unit) {}

fun test() {
    bar(Data(null)) { it.dataMethod() }
}