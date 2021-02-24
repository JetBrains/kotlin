class A {
    fun a() {}

    open class B(i: Int)

    <caret>inner class C(i: Int) : B(i)
}