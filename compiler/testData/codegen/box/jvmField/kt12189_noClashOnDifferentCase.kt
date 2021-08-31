// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB

abstract class Base {
    @JvmField val name: String = "O"
    @JvmField val Name: String = "K"
}

class Derived : Base()

fun box(): String =
    Derived().name + Derived().Name
