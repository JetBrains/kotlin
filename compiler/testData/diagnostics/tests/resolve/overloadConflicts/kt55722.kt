// FIR_IDENTICAL
// FIR_DUMP
// SKIP_TXT

interface A<T> {
    fun foo(x: T?) {}
}

interface B<F> : A<F> {
    override fun foo(x: F?)
}

fun <T> bar(x: A<in T>) {
    if (x is B) {
        x.foo(null) // Shouldn't be OVERLOAD_RESOLUTION_AMBIGUITY
    }
}
