// TARGET_BACKEND: JVM
// ISSUE: KT-54645

abstract class Base {
    open class Nested
}

sealed class Derived : Base() {
    open class Nested
}

class Impl() : Derived() {
    companion object : Nested()
}

fun takeDerivedNested(x: Derived.Nested) {}

fun box(): String {
    takeDerivedNested(Impl)
    return "OK"
}
