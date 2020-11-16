// ISSUE: KT-13495
// !LANGUAGE: +FreedomForSealedClasses
// !DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: base.kt

package foo

class Container {
    sealed class Base
}

// FILE: a.kt

package foo

class A : <!HIDDEN, SEALED_SUPERTYPE!>Container.Base<!>()

// FILE: b.kt

package foo

class BContainer {
    class B : <!HIDDEN, SEALED_SUPERTYPE!>Container.Base<!>()

    inner class C : <!HIDDEN, SEALED_SUPERTYPE!>Container.Base<!>()
}

// FILE: test.kt

package foo

fun test(base: Container.Base) {
    val x = when (base) {
        is A -> 1
        is BContainer.B -> 2
        is BContainer.C -> 3
    }
}
