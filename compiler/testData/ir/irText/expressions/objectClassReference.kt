// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// WITH_STDLIB
object A

fun test() {
    A::class
    A::class.java
}