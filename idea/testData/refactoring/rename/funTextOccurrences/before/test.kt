package test

class A {
    fun /*rename*/foo() {}

    fun bar() {
        foo()
    }
}