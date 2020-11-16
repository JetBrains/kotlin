// ISSUE: KT-13495
// !LANGUAGE: +FreedomForSealedClasses

// FILE: a.kt

sealed class Base {
    class A : Base()
}

// FILE: b.kt

class B : Base()

// FILE: c.kt

class Container {
    class C : Base()

    inner class D : Base()

    val anon = object : <!SEALED_SUPERTYPE!>Base<!>() {} // Should be an error

    fun someFun() {
        class LocalClass : <!SEALED_SUPERTYPE!>Base<!>() {} // Should be an error
    }
}
