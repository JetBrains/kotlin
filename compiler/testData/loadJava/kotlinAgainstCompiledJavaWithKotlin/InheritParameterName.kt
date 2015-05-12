package test

interface B {
    fun foo(kotlinName: Int)
}

abstract class ZAB : A, B
abstract class ZBA : B, A
