// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// WITH_STDLIB
class A

fun test() {
    A::class
    A()::class

    A::class.java
    A()::class.java
}