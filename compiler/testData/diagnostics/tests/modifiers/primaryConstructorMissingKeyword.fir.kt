// !DIAGNOSTICS: -UNUSED_PARAMETER
annotation class Ann(val x: Int = 1)
class A <!MISSING_CONSTRUCTOR_KEYWORD!>private (val x: Int)<!> {
    inner class B <!MISSING_CONSTRUCTOR_KEYWORD!>@Ann(2) (val y: Int)<!>

    fun foo() {
        class C <!MISSING_CONSTRUCTOR_KEYWORD!>private @Ann(3) (args: Int)<!>
    }
}
