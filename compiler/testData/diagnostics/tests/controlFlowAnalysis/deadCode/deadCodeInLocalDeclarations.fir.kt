// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testObject() {
    object : <!UNREACHABLE_CODE!>Foo(<!>todo()<!UNREACHABLE_CODE!>)<!> {
        fun foo() = 1
    }
}

fun testObjectExpression() {
    val <!UNUSED_VARIABLE!>a<!>  = object : <!UNREACHABLE_CODE!>Foo(<!>todo()<!UNREACHABLE_CODE!>)<!> {
        fun foo() = 1
    }
}

fun testObjectExpression1() {
    fun bar(i: Int, x: Any) {}

    bar(1, object : <!UNREACHABLE_CODE!>Foo(<!>todo()<!UNREACHABLE_CODE!>)<!> {
        fun foo() = 1
    })
}

fun testClassDeclaration() {
    class C : <!UNREACHABLE_CODE!>Foo(<!>todo()<!UNREACHABLE_CODE!>)<!> {}

    bar()
}

fun testFunctionDefaultArgument() {
    fun foo(x: Int = todo()) { bar() }
}

open class Foo(i: Int) {}

fun todo(): Nothing = throw Exception()
fun bar() {}
