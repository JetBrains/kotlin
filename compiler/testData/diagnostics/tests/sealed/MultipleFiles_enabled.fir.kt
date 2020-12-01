// ISSUE: KT-13495
// !LANGUAGE: +AllowSealedInheritorsInDifferentFilesOfSamePackage

// FILE: a.kt

package foo

sealed class Base {
    class A : Base()
}

// FILE: b.kt

package foo

class B : <!HIDDEN!>Base<!>()

// FILE: c.kt

package foo

class Container {
    class C : <!HIDDEN, SEALED_SUPERTYPE!>Base<!>()

    inner class D : <!HIDDEN, SEALED_SUPERTYPE!>Base<!>()

    val anon = object : <!HIDDEN, SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Base<!>() {} // Should be an error

    fun someFun() {
        class LocalClass : <!HIDDEN, SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Base<!>() {} // Should be an error
    }
}

// FILE: E.kt

package bar

import foo.Base

class E : <!HIDDEN!>Base<!>()
