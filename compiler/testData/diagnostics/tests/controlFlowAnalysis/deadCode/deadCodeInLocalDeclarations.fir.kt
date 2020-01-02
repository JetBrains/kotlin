// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testObject() {
    object : Foo(todo()) {
        fun foo() = 1
    }
}

fun testObjectExpression() {
    val a  = object : Foo(todo()) {
        fun foo() = 1
    }
}

fun testObjectExpression1() {
    fun bar(i: Int, x: Any) {}

    bar(1, object : Foo(todo()) {
        fun foo() = 1
    })
}

fun testClassDeclaration() {
    class C : Foo(todo()) {}

    bar()
}

fun testFunctionDefaultArgument() {
    fun foo(x: Int = todo()) { bar() }
}

open class Foo(i: Int) {}

fun todo(): Nothing = throw Exception()
fun bar() {}