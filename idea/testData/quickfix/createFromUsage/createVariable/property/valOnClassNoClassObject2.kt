// "Create member property 'A.Companion.foo'" "true"
// ERROR: Property must be initialized or be abstract

class A

fun test() {
    val a: Int = A.<caret>foo
}
