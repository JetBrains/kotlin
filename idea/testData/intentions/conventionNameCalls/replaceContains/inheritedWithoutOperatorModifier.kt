open class A {
    open fun contains(x: Int) : Boolean = true
}

open class B : A() {
    override fun contains(x: Int) : Boolean = false
}

fun foo() {
    val b = B()
    b.cont<caret>ains(9)
}