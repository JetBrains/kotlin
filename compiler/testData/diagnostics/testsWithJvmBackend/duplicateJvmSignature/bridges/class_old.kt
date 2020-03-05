// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_OLD

open class B<T> {
    open fun foo(t: T) {}
}

class <!CONFLICTING_JVM_DECLARATIONS!>C<!> : B<String>() {
    override fun foo(t: String) {}

    <!CONFLICTING_JVM_DECLARATIONS!>fun foo(o: Any)<!> {}
}