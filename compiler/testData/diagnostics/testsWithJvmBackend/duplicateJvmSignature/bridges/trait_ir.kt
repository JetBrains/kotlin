// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_IR

interface B<T> {
    fun foo(t: T) {}
}

class C : B<String> {
    override fun foo(t: String) {}

    <!ACCIDENTAL_OVERRIDE!>fun foo(o: Any)<!> {}
}