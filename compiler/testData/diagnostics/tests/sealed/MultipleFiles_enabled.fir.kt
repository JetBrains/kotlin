// ISSUE: KT-13495
// !DIAGNOSTICS: -UNUSED_VARIABLE
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
}

// FILE: d.kt

fun test(base: Base) {
    val x = when (base) {
        is Base.A -> 1
        is B -> 2
    }
}
