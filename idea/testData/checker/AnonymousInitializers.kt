trait NoC {
  <error>{

  }</error>

  val a : Int get() = 1

  <error>{

  }</error>
}

class WithC() {
  val x : Int
  {
    $x = 1
    <error>$y</error> = 2
    val <warning>b</warning> = x

  }

  val a : Int get() = 1

  {
    val <warning>z</warning> = <error>b</error>
    val <warning>zz</warning> = x
    val <warning>zzz</warning> = <error>$a</error>
  }

}