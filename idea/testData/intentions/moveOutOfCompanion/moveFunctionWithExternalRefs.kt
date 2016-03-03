// WITH_RUNTIME
// SHOULD_FAIL_WITH: Class instance required: foo, Class instance required: foo, Class instance required: foo, Class instance required: foo
class A {
    companion object {
        class B {
            init {
                foo()
            }
        }

        fun <caret>foo() {

        }
    }

    fun bar() {
        foo()
    }
}

fun test() {
    A.foo()
    A.Companion.foo()
    with(A) {
        foo()
    }
}