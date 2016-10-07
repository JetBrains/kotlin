open class A {
    open fun foo(n: Int) {

    }

    open fun bar(n: Int) {

    }
}

open class <caret>B : A() {
    // INFO: {"checked": "true"}
    fun foo() {
        super.foo(1)
        super.bar(1)
    }

    // INFO: {"checked": "true"}
    override fun foo(n: Int) {

    }

    override fun bar(n: Int) {

    }
}

class C : B() {

}