// "Create property 'foo'" "false"
// ACTION: Split property declaration
// ERROR: Unresolved reference: foo

class A<T>(val n: T)

fun test() {
    val a: Int = A.<caret>foo
}
