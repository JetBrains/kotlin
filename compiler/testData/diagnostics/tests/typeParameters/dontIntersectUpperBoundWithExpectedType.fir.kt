// !DIAGNOSTICS: -UNUSED_VARIABLE

open class Foo

class Bar

fun <T : Foo> foo(): T? {
    return null
}

fun main() {
    val a: Bar? = <!DEBUG_INFO_EXPRESSION_TYPE("Foo? & Bar?")!>foo()<!>
}


fun <T : Appendable> wtf(): T = TODO()
val bar: Int = wtf() // happily compiles
