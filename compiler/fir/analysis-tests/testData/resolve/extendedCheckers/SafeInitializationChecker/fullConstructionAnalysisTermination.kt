open class B {
    class C : B()

    val c: C = C()
}

open class A {
    inner class C : B()

    val c: C = C()
}