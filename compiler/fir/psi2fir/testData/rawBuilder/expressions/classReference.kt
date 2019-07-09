//WITH_RUNTIME
package test

class A

fun test() {
    A::class
    test.A::class
    A()::class

    A::class.java
    test.A::class.java
    A()::class.java
}