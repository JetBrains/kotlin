class A {
    fun f1() {}
}

fun myA(func: A.() -> Unit) {
    A().func()
}

fun main() {
    myA foo@{
        <caret>this@foo.f1()
    }
}