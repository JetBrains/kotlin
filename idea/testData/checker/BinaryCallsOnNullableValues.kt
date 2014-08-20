class A() {
  override fun equals(<warning>a</warning> : Any?) : Boolean = false
}

fun f(): Unit {
  var x: Int? = <warning>1</warning>
  x = 1
  x <error>+</error> 1
  x <error>plus</error> 1
  x <error><</error> 1
  x <error>+=</error> 1

  x == 1
  x != 1

  <error>A() == 1</error>

  <error>x === "1"</error>
  <error>x !== "1"</error>

  x === 1
  x !== 1

  x<error>..</error>2
  <error>x</error> in 1..2

  val y : Boolean? = true
  <warning>false || <error>y</error></warning>
  <warning><error>y</error> && true</warning>
  <warning><error>y</error> && <error>1</error></warning>
}