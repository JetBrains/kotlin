// !DIAGNOSTICS: -UNUSED_PARAMETER

interface B<T> {
    fun foo(t: T) {}
}

class C : B<String> {
    override fun foo(t: String) {}

    fun foo(o: Any) {}
}