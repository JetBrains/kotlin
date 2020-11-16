// ISSUE: KT-13495
// !LANGUAGE: +FreedomForSealedClasses

// FILE: a.kt

sealed class Base {
    class A : Base()
}

// FILE: b.kt

class B : <!HIDDEN!>Base<!>()

// FILE: c.kt

class Container {
    class C : <!HIDDEN, SEALED_SUPERTYPE!>Base<!>()

    inner class D : <!HIDDEN, SEALED_SUPERTYPE!>Base<!>()

    val anon = object : <!HIDDEN, SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Base<!>() {} // Should be an error

    fun someFun() {
        class LocalClass : <!HIDDEN, SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Base<!>() {} // Should be an error
    }
}
