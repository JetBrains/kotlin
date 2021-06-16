interface C1 {
    fun foo() {}
}
interface C2 {
    fun foo() {}
}

context(C1, C2)
fun bar() {
    foo()
}