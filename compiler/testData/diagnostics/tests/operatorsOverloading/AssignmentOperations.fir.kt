class A {
  operator fun plusAssign(x: Int) {}
  operator fun minusAssign(x: Int) {}
  operator fun timesAssign(x: Int) {}
  operator fun divAssign(x: Int) {}
  operator fun remAssign(x: Int) {}
}

fun testVal() {
  val a = A()
  a += 1
  a -= 1
  a *= 1
  a /= 1
  a %= 1
}

fun testExpr() {
  A() += 1
  A() -= 1
  A() *= 1
  A() /= 1
  A() %= 1
}

class B {
  operator fun plus(x: Int): B = B()
  operator fun minus(x: Int): B = B()
  operator fun times(x: Int): B = B()
  operator fun div(x: Int): B = B()
  operator fun rem(x: Int): B = B()
}

fun testWrong() {
  <!VARIABLE_EXPECTED!>B()<!> += 1
  <!VARIABLE_EXPECTED!>B()<!> -= 1
  <!VARIABLE_EXPECTED!>B()<!> *= 1
  <!VARIABLE_EXPECTED!>B()<!> /= 1
  <!VARIABLE_EXPECTED!>B()<!> %= 1
}