// COMPARE_WITH_LIGHT_TREE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface B<T> {
    fun foo(t: T) {}
}

class C : B<String> {
    <!ACCIDENTAL_OVERRIDE{LT}!><!ACCIDENTAL_OVERRIDE{PSI}!>fun foo(o: Any)<!> {}<!>
}
