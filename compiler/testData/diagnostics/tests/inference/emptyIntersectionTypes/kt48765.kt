// RENDER_DIAGNOSTICS_FULL_TEXT
open class A<T1, T2> {}
class B {
    fun <T1: Number, T2: A<Float, T1>> foo(x1: T2, x2: T1) {}
}
class C<T: D, T2>(val x: T, val y: T2) {
    fun test() {
        B().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR!>foo<!>(x, foo())
    }
}
open class D: A<Float, Number>()
fun <T: <!FINAL_UPPER_BOUND!>String<!>> foo(): T  {
    return "" <!UNCHECKED_CAST!>as T<!> // this cast is safe because String is final.
}
fun main() {
    C(D(), 10.5).test()
}
