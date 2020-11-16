// ISSUE: KT-13495
// !DIAGNOSTICS: -UNUSED_VARIABLE
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

// FILE: d.kt

fun test(base: Base) {
    val x = when (base) {
        is Base.A -> 1
        is B -> 2
        is Container.C -> 3
        is Container.D -> 4
    }
}
