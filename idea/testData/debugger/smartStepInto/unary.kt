class A {
    fun plus() {}
    fun minus() {}
}

fun foo() {
    +A() || A()-<caret>
}
// EXISTS: plus(), minus()