// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    fun bar(x: String, y: String = x) {}
    fun baz(x: Int = <!UNRESOLVED_REFERENCE!>y<!>, y: Int) {}
}