// WITH_RUNTIME
// SHOULD_FAIL_WITH: 'FOO' in property bar2 will require class instance, 'FOO' in property bar4 will require class instance, 'FOO' in property bar6 will require class instance, 'FOO' in variable bar7 will require class instance, 'FOO' in variable bar8 will require class instance
package test

open class A {
    companion object {
        val <caret>FOO = 1
    }
}

class B : A() {
    val bar1 = FOO

    class X {
        val bar2 = FOO
    }

    class W : A() {
        val bar3 = FOO
    }

    object Y {
        val bar4 = FOO
    }

    inner class Z {
        val bar5 = FOO
    }

    companion object {
        val bar6 = FOO
    }
}

class C {
    inner class D : A() {
        val bar9 = FOO
    }
}

fun foo() {
    val bar7 = A.FOO
    with(A) {
        val bar8 = FOO
    }
}