class A() {
  override fun equals(a : Any?) : Boolean = false
}

fun f(): Unit {
  var x: Int? = 1
  x = 1
  x + 1
  x.plus(1)
  x < 1
  x += 1

  x == 1
  x != 1

  A() == 1

  x === "1"
  x !== "1"

  x === 1
  x !== 1

  x..2
  x in 1..2

  val y : Boolean? = true
  false || y
  y && true
  y && 1
}
