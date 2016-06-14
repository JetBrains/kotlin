package test

open class A {
    val /*rename*/foo = 1
}

open class B : A() {
    val bar = 2
}