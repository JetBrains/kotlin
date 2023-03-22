// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
interface NoC {
  <!ANONYMOUS_INITIALIZER_IN_INTERFACE!>init<!> {

  }

  val a : Int get() = 1

  <!ANONYMOUS_INITIALIZER_IN_INTERFACE!>init<!> {

  }
}

class WithC() {
  val x : Int = 1
  init {
    val b = x

  }

  val a : Int get() = 1

  init {
    val z = <!UNRESOLVED_REFERENCE!>b<!>
    val zz = x
  }
}
