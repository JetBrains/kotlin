// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val a by <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>a<!>

val b by Delegate(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>b<!>)

val c by <!UNINITIALIZED_VARIABLE, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>d<!>
val d by <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>c<!>

class Delegate(i: Int) {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
}
