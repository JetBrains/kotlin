package test

trait B {
    fun foo(kotlinName: Int)
}

class ZAB : A, B
class ZBA : B, A
