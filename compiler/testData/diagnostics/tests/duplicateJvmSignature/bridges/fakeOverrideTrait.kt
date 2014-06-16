// !DIAGNOSTICS: -UNUSED_PARAMETER

trait B<T> {
    fun foo(t: T) {}
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : B<String> {
    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(o: Any)<!> {}
}