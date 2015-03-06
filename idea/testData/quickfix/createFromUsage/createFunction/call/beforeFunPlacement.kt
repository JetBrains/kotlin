// "Create function 'foo'" "true"

class A {
    val baz = 1

    fun test() {
        val a: A = <caret>foo()
    }

    fun bar() {

    }
}

