// !WITH_NEW_INFERENCE

abstract class Abstract

fun <D> create(fn: () -> D): D {
    return fn()
}

fun main() {
    create(::<!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS, OI;CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS, OI;CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Abstract<!>)
}
