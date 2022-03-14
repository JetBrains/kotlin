// FILE: StarImported.kt

package star

class SomeClass

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

fun useSomeClass(): SomeClass = <!RESOLUTION_TO_CLASSIFIER!>SomeClass<!>()

fun useAnotherClass(): AnotherClass = <!RESOLUTION_TO_CLASSIFIER!>AnotherClass<!>()

fun test() {
    foo()
    baz()
    val x = bar
    val y = gau
}
