// "Create extension property 'A.foo'" "true"
// ERROR: Property must be initialized
class A(val n: Int)

class B {
    val A.test: Boolean get() = <caret>foo
}