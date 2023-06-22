// COMPARE_WITH_LIGHT_TREE
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class B<T> {
    open fun foo(t: T) {}
}

class C : B<String>() {
    override fun foo(t: String) {}

    <!ACCIDENTAL_OVERRIDE!>fun foo(o: Any)<!> {}
}
