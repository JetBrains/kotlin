package test

open class A(val /*rename*/foo: Int)

open class B : A() {
    val bar = 2
}