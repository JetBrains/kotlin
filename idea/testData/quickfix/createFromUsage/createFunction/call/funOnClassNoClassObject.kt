// "Create member function 'foo'" "false"
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

class A<T>(val n: T)

fun test() {
    val a: Int = A.<caret>foo(2)
}