interface NoC {
  init {

  }

  val a : Int get() = 1

  init {

  }
}

class WithC() {
  val x : Int = 42
  init {
    val b = x

  }

  val a : Int get() = 1

  init {
    val z = <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: b">b</error>
    val zz = x
  }

}
