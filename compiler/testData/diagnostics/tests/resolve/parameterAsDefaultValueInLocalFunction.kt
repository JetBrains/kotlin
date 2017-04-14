// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    fun bar(x: String, y: String = x) {}
    fun baz(x: Int = <!UNINITIALIZED_PARAMETER!>y<!>, y: Int) {}
}