// FIR_IDENTICAL
// WITH_STDLIB

abstract class Foo

object A {
    fun Foo.contains(vararg xs: Int) = // 1
        xs.forEach(this::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>contains<!>) // resolved to (2) in 1.7.0-RC, should be error "Type checking has run into a recursive problem"
}

fun Any.contains(vararg xs: Int) {} // 2

fun box(): String {
    object : Foo() {}.contains(1)
    return "OK"
}