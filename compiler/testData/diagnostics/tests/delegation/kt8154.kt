// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
interface A<T> {
    fun foo()
}

interface B<T> : A<T> {
    fun bar()
}

class BImpl<T>(a: A<T>) : B<T>, A<T> by a {
    override fun bar() { throw UnsupportedOperationException() }
}
