open class A() {
    open inner class InnerA
}

class B : A() {
    inner class InnerB : A.InnerA()
}

fun box(): String {
    B().InnerB()
    return "OK"
}
