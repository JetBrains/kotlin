package one.two

import one.two.Base.NestedClass

open class Base<B : Base.NestedClass> {
    open class NestedClass
    inner class Inner
}

class Child<C : one.two.Base.NestedClass> : Base<C>() {
    fun function(): NestedClass? = null
    fun classTypeParameter(): C? = null
}

fun local(c1: Child<NestedClass>, c2: one.two.Child<one.two.Base.NestedClass>) {
    val nested: NestedClass = NestedClass()
    val inner = c1.Inner()
}