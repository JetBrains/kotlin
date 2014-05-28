package test

trait B {
    fun foo(kotlinName: Int)
}

abstract class ZAB : A, B
abstract class ZBA : B, A
