// IS_APPLICABLE: false
open class A {
    <caret>internal fun foo() {}

    fun bar(c: C) {
        c.foo()
    }
}

class C : A()