// "Add name to argument: 'b = B()'" "true"
// LANGUAGE_VERSION: 1.3

open class A {}
open class B : A() {}

fun f(a: Int, b: A) {}
fun g() {
     f(a=1, <caret>B())
}