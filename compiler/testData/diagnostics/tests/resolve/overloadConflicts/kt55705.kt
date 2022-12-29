// FIR_IDENTICAL
// SKIP_TXT

interface A<T> {
    fun foo(x: T?) {}
}

interface B : A<String> {
    override fun foo(x: String?)
}

fun <T> bar(x: A<in T>, t: String?) {
    if (x is B) {
        x.foo(null) // Ok in K1, was OVERLOAD_RESOLUTION_AMBIGUITY in K2
    }
}
