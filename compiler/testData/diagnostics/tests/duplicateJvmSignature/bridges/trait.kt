// !DIAGNOSTICS: -UNUSED_PARAMETER

interface B<T> {
    fun foo(t: T) {}
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : B<String> {
    override fun foo(t: String) {}

    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(o: Any)<!> {}
}