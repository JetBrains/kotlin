// !DIAGNOSTICS: -UNUSED_PARAMETER

open class B<T> {
    open fun foo(t: T) {}
}

class C : B<String>() {
    override fun foo(t: String) {}

    fun foo(o: Any) {}
}