// WITH_STDLIB
// MODULE: main
// FILE: main.kt

interface Foo {
    /**
     * KDoc for abstract fun
     */
    fun documented()

    fun undocumented
}

class A : Foo {
    /**
     * Override documentation for [documented]
     */
    override fun documented() {}

    /**
     * New documentation for [undocumented]
     */
    override fun undocumented() {}
}

class B : Foo {
    override fun documented() {}
    override fun undocumented() {}
}

open class Parent {
    /**
     * parent property docs
     */
    open var v = 0
}

class Child : Parent() {
    /**
     * child property docs
     */
    override var v = 0
}

class GrandChild : Parent() {
    override var v = 0
}

fun foo() {
    val fooA: Foo = A()
    val fooB: Foo = B()

    val a = A()
    val b = B()

    fooA.documented()
    fooB.documented()

    fooA.undocumented()
    fooB.undocumented()

    a.documented()
    a.undocumented()

    b.documented()
    b.undocumented()
}