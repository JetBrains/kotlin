
package test.pkg

open class Parent {
    open fun foo(param: Int = 0) = Unit
}

class Child: Parent() {
    override fun foo(pa<caret>ram: Int) = Unit
}