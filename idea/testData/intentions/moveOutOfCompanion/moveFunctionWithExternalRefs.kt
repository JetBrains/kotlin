// WITH_RUNTIME
// SHOULD_FAIL_WITH: 'foo' in class B will require class instance, 'foo' in function test() will require class instance, 'foo' in function test() will require class instance, 'foo' in lambda &lt;anonymous&gt;() will require class instance
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