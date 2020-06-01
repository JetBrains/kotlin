package test

abstract class A {
    inner class Inner(val x: String)
}
abstract class B : A()

