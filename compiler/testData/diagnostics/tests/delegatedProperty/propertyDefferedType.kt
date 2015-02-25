// !DIAGNOSTICS: -UNUSED_PARAMETER

class B {
    val c by Delegate(<!UNRESOLVED_REFERENCE!>ag<!>)
}

class Delegate<T: Any>(val init: T) {
  fun get(t: Any?, p: PropertyMetadata): Int = null!!
}
