// TARGET_BACKEND: JVM
// WITH_STDLIB
class A

fun test() {
    A::class
    A()::class

    A::class.java
    A()::class.java

    Array<String>::class
    Array<Array<IntArray>?>::class
}
