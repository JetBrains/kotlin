// DIAGNOSTICS: -UNUSED_PARAMETER

open class B<T> {
    open fun foo(t: T) {}
}

class C : B<String>() {
    override fun <!ACCIDENTAL_OVERLOAD_CLASH_BY_JVM_ERASURE_WARNING!>foo<!>(t: String) {}

    <!ACCIDENTAL_OVERRIDE!>fun foo(o: Any) {}<!>
}
