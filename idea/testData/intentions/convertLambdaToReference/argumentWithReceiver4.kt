package p

class A {
    fun f1() {
    }
}

fun myA(func: A.() -> Unit) {
    A().func()
}

fun main() {
    myA({<caret> f1() })
}