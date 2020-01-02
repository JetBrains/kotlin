// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
package test

class A {
    object Companion

    companion object
}

class B {
    companion object Named

    object Named
}

class C {
    class Named

    companion object Named
}