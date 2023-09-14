// ISSUE: KT-13495
// !LANGUAGE: +AllowSealedInheritorsInDifferentFilesOfSamePackage

// FILE: a.kt

package foo

sealed class Base {
    class A : Base()
}

// FILE: b.kt

package foo

class B : Base()

// FILE: c.kt

package foo

class Container {
    class C : Base()

    inner class D : Base()

    val anon = object : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Base<!>() {} // Should be an error

    fun someFun() {
        class LocalClass : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Base<!>() {} // Should be an error
    }
}

// FILE: E.kt

package bar

import foo.Base

typealias TA = Base

class E : <!SEALED_INHERITOR_IN_DIFFERENT_PACKAGE!>Base<!>()
class E2 : <!SEALED_INHERITOR_IN_DIFFERENT_PACKAGE!>TA<!>()
