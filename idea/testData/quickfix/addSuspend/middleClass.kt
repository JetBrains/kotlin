// "Add 'suspend' modifier to all functions in hierarchy" "true"
open class A {
    open fun foo() {

    }

    open fun foo(n: Int) {

    }
}

open class B : A() {
    override suspend fun <caret>foo() {

    }

    override fun foo(n: Int) {

    }
}

open class B1 : A() {
    override fun foo() {

    }

    override fun foo(n: Int) {

    }
}

open class C : B() {
    override fun foo() {

    }

    override fun foo(n: Int) {

    }
}

open class C1 : B() {
    override fun foo() {

    }

    override fun foo(n: Int) {

    }
}