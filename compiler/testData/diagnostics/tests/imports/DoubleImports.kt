// FIR_IDENTICAL
// FILE: StarImported.kt

package star

class SomeClass {
    class Nested
}

fun foo() {}

val bar = 1

// FILE: ExplicitImported.kt

package explicit

class AnotherClass

fun baz() {}

val gau = 2

// FILE: Test.kt

import star.*
import star.*
import explicit.<!CONFLICTING_IMPORT!>AnotherClass<!>
import explicit.<!CONFLICTING_IMPORT!>AnotherClass<!>
import explicit.baz
import explicit.baz
import explicit.gau
import explicit.gau

fun useSomeClass(): SomeClass = SomeClass()

fun useNested(): SomeClass.Nested = SomeClass.Nested()

fun useAnotherClass(): AnotherClass = AnotherClass()

fun test() {
    foo()
    baz()
    val x = bar
    val y = gau
}
