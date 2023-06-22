// FIR_IDENTICAL
// ISSUE: KT-55705
// FIR_DUMP

interface A<T> {
    fun foo(x: T?) {}
}

interface B : A<String> {
    override fun foo(x: String?)
}

fun <T> bar(x: A<in T>) {
    if (x is B) {
        x.foo(null) // Should be no OVERLOAD_RESOLUTION_AMBIGUITY
    }
}