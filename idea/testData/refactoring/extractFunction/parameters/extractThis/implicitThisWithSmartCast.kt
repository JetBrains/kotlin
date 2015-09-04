// SUGGESTED_NAMES: i, getB
// PARAM_DESCRIPTOR: public fun A.ext(): kotlin.Unit defined in root package
// PARAM_TYPES: B
fun main(args: Array<String>) {
    val a: A = B()
    a.ext()
}

fun A.ext() {
    if (this !is B) return
    val b = <selection>foo()</selection>
}

open class A
class B: A() {
    fun foo() = 1
}