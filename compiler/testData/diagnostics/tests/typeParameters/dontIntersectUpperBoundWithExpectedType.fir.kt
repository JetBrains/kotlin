// !DIAGNOSTICS: -UNUSED_VARIABLE

open class Foo

class Bar

fun <T : Foo> foo(): T? {
    return null
}

fun main() {
    val a: Bar? = <!DEBUG_INFO_EXPRESSION_TYPE("Foo?"), INITIALIZER_TYPE_MISMATCH, NEW_INFERENCE_ERROR!>foo()<!>
}


fun <T : Appendable> wtf(): T = TODO()
val bar: Int = <!INITIALIZER_TYPE_MISMATCH, NEW_INFERENCE_ERROR!>wtf()<!> // happily compiles
