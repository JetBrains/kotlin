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
    val <!UNUSED_VARIABLE!>b<!> = x

  }

  val a : Int get() = 1

  init {
    val <!UNUSED_VARIABLE!>z<!> = <!UNRESOLVED_REFERENCE!>b<!>
    val <!UNUSED_VARIABLE!>zz<!> = x
  }
}
