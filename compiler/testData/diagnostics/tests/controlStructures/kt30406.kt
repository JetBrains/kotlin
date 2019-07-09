// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !CHECK_TYPE
// Issue: KT-30406

interface Option<out T> {
    val s: String
}
class Some<T>(override val s: String) : Option<T>
class None(override val s: String = "None") : Option<Int>

fun test(a: Int): Option<Any> =
    <!DEBUG_INFO_EXPRESSION_TYPE("Option<kotlin.Any>")!>if (a == 239)
        <!DEBUG_INFO_EXPRESSION_TYPE("Some<kotlin.Any>")!>Some("239")<!>
    else
        <!DEBUG_INFO_EXPRESSION_TYPE("None")!>None()<!><!>