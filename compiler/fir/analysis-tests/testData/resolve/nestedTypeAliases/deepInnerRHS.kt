// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73752

// FILE: main.kt

class Outer {
    inner class Inner {
        inner class InnerInner

        <!NESTED_CLASS_NOT_ALLOWED!>class InnerNested<!> // NESTED_CLASS_NOT_ALLOWED
    }

    class Nested {
        inner class NestedInner
    }

    typealias TAtoInner = Inner
    typealias TAtoInnerInner = Inner.InnerInner
    typealias TAtoNested = Nested
    typealias TAtoNestedInner = Nested.NestedInner

    fun test() {
        TAtoInner() // OK
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAtoInnerInner<!>() // UNRESOLVED_REFERENCE_WRONG_RECEIVER
        TAtoNested() // OK
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAtoNestedInner<!>() // UNRESOLVED_REFERENCE_WRONG_RECEIVER
    }
}

// FILE: import.kt

import Outer.TAtoInner
import Outer.TAtoInnerInner
import Outer.TAtoNested
import Outer.TAtoNestedInner

val nestedTAtoInner = Outer().TAtoInner()
val nestedTAtoInnerCallable = Outer::TAtoInner
val nestedTAtoInnerInnerError = Outer().<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAtoInnerInner<!>()
val nestedTAtoInnerInnerCallableError = Outer::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAtoInnerInner<!>
val nestedTAtoInnerInner = Outer().Inner().TAtoInnerInner()
val nestedTAtoInnerInnerCallable = Outer.Inner::TAtoInnerInner
val nestedTAtoNested = Outer.TAtoNested()
val nestedTAtoNestedCallable = Outer::TAtoNested
val nestedTAtoNestedInner = Outer.Nested().TAtoNestedInner()
val nestedTAtoNestedInnerCallable = Outer.Nested::TAtoNestedInner
val nestedTAtoNestedInnerError = Outer.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>TAtoNestedInner<!>()
val nestedTAtoNestedInnerCallableError = Outer::<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>TAtoNestedInner<!>
