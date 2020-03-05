class A {
    fun a() {}

    open class B

    <caret>inner class C : B()
}