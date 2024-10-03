// FIR_IDENTICAL
// ISSUE: KT-71966

package a

abstract class A : <!CYCLIC_INHERITANCE_HIERARCHY!>C<!>() {
    abstract class Nested
}

abstract class C : <!CYCLIC_INHERITANCE_HIERARCHY!>A.Nested<!>()
