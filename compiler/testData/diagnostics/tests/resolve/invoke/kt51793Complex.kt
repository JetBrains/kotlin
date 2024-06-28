// FIR_IDENTICAL
// ISSUE: KT-51793

interface Key1
interface Key2

interface A1 {
    operator fun Key2.invoke(): String = ""
}
interface A2 {
    operator fun Key1.invoke(): Int = 1
}

val A1.k: Key1 get() = object : Key1 {}
val A2.k: Key2 get() = object : Key2 {}

fun with1(a: A1.() -> Unit) {
    a(object : A1 {})
}
fun with2(a: A2.() -> Unit) {
    a(object : A2 {})
}

fun main() {
    with1 {
        with2 {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>k()<!>
        }
    }
}