// WITH_RUNTIME
// SHOULD_FAIL_WITH: 'foo' in class B will require class instance, 'foo' in function test() will require class instance, 'foo' in function test() will require class instance, 'foo' in lambda &lt;anonymous&gt;() will require class instance
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