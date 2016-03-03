// WITH_RUNTIME
// SHOULD_FAIL_WITH: Class instance required: foo, Class instance required: foo, Class instance required: foo, Class instance required: foo
class A {
    companion object {
        class B {
            init {
                foo + 1
            }
        }

        val <caret>foo: Int = 1
    }

    fun bar() {
        foo + 1
    }
}

fun test() {
    A.foo + 1
    A.Companion.foo + 1
    with(A) {
        foo + 1
    }
}