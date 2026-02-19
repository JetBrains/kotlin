package r

class A {
    open inner class Base(val x: Int)
    inner class B(x: Int): Base(x)
}
