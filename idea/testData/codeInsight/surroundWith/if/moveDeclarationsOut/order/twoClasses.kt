fun foo() {
    <selection>"start"
    open class A {}
    class B: A() {}
    "end"</selection>

    val c = B()
    val d = A()
}