// !DIAGNOSTICS: -UNUSED_VARIABLE

open class Foo

class Bar

fun <T : Foo> foo(): T? {
    return null
}

fun main() {
    val a: Bar? = <!DEBUG_INFO_EXPRESSION_TYPE("Foo? & Bar?")!><!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR!>foo<!>()<!>
}


fun <T : Appendable> wtf(): T = TODO()
val bar: Int = <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>wtf<!>() // happily compiles
