// "Create extension property 'A.foo'" "true"
// ERROR: Property must be initialized
class A(val n: Int)

class B {
    var A.test: Boolean
        get() = foo
        set(v: Boolean) {
            <caret>foo = v
        }
}