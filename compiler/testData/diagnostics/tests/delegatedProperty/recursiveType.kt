// !DIAGNOSTICS: -UNUSED_PARAMETER

val a by <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>a<!>

val b by Delegate(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>b<!>)

val c by <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>d<!>
val d by <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>c<!>

class Delegate(i: Int) {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}