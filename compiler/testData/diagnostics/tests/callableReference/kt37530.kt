// FIR_IDENTICAL

abstract class Abstract

fun <D> create(fn: () -> D): D {
    return fn()
}

fun main() {
    create(::<!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Abstract<!>)
}
