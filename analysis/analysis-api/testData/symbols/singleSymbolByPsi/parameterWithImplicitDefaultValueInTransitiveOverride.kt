
package test.pkg

open class A {
    open fun foo(param: Int = 0) = Unit
}

open class B : A() {
    override fun foo(param: Int) = Unit
}

class C : B() {
    override fun foo(pa<caret>ram: Int) = Unit
}
