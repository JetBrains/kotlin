fun foo1() :  (Int) -> Int = { x: Int -> x }

fun foo() {
  val h :  (Int) -> Int = foo1();
  h(1)
  val m :  (Int) -> Int = {<warning descr="[UNUSED_PARAMETER] Parameter 'a' is never used">a</warning> : Int -> 1}//foo1()
  m(1)
}
