// FIR_IDENTICAL
// WITH_NEW_INFERENCE
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: packageA.kt

package a

abstract class Cls
abstract class Cls2

// FILE: packageB.kt

package b

fun Cls() {}
class Cls2

// FILE: test.kt

package c

import a.*
import b.*

fun take(arg: Any) {}

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>Cls<!>()
    take(<!OVERLOAD_RESOLUTION_AMBIGUITY!>Cls<!>())

    <!UNRESOLVED_REFERENCE!>Cls2<!>()
    take(<!UNRESOLVED_REFERENCE!>Cls2<!>())
}
