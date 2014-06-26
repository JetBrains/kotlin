// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testObject() {
    <!UNREACHABLE_CODE!>object : Foo(<!>todo()<!UNREACHABLE_CODE!>) {
        fun foo() = 1
    }<!>
}

fun testObjectExpression() {
    <!UNREACHABLE_CODE!>val <!UNUSED_VARIABLE!>a<!>  = object : Foo(<!>todo()<!UNREACHABLE_CODE!>) {
        fun foo() = 1
    }<!>
}

fun testObjectExpression1() {
    fun bar(i: Int, x: Any) {}

    <!UNREACHABLE_CODE!>bar(<!>1, <!UNREACHABLE_CODE!>object : Foo(<!>todo()<!UNREACHABLE_CODE!>) {
        fun foo() = 1
    })<!>
}

fun testClassDeclaration() {
    class C : <!UNREACHABLE_CODE!>Foo(<!>todo()<!UNREACHABLE_CODE!>)<!> {}

    <!UNREACHABLE_CODE!>bar()<!>
}

fun testFunctionDefaultArgument() {
    fun foo(x: Int = todo()) { bar() }
}

open class Foo(i: Int) {}

fun todo() = throw Exception()
fun bar() {}