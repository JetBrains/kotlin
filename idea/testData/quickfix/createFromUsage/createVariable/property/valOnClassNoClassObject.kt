// "Create property 'foo'" "false"
// ERROR: Unresolved reference: foo

class A<T>(val n: T)

fun test() {
    val a: Int = A.<caret>foo
}
