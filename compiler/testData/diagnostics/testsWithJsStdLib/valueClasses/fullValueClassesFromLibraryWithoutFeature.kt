// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -PRE_RELEASE_CLASS
// MODULE: lib
// LANGUAGE: +FullValueClasses
// FILE: lib.kt
package lib

value class Multi(val a: Int, val b: Int)

abstract value class Base

value class Sub(val x: Int) : Base()

// MODULE: main(lib)
// FILE: main.kt
import lib.*

fun test(multi: Multi, sub: Sub, base: Base) {
    <!FORBIDDEN_IDENTITY_EQUALS!>multi === multi<!>
    <!FORBIDDEN_IDENTITY_EQUALS!>sub === sub<!>
    <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>base === base<!>
}
