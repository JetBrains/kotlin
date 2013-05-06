// "Add name to argument: 'b = B()'" "true"
open class A {}
open class B : A() {}

fun f(a: Int, b: A) {}
fun g() {
     f(a=1, <caret>B())
}