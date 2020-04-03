interface NoC {
  init {

  }

  val a : Int get() = 1

  init {

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
