trait NoC {
  <error>init {

  }</error>

  val a : Int get() = 1

  <error>init {

  }</error>
}

class WithC() {
  val x : Int
  init {
    $x = 1
    <error>$y</error> = 2
    val <warning>b</warning> = x

  }

  val a : Int get() = 1

  init {
    val <warning>z</warning> = <error>b</error>
    val <warning>zz</warning> = x
    val <warning>zzz</warning> = <error>$a</error>
  }

}