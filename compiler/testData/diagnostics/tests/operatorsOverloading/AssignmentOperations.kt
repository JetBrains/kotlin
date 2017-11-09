class A {
  operator fun plusAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
  operator fun minusAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
  operator fun timesAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
  operator fun divAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
  operator fun remAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
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
  operator fun plus(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
  operator fun minus(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
  operator fun times(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
  operator fun div(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
  operator fun rem(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
}

fun testWrong() {
  <!VARIABLE_EXPECTED!>B()<!> += 1
  <!VARIABLE_EXPECTED!>B()<!> -= 1
  <!VARIABLE_EXPECTED!>B()<!> *= 1
  <!VARIABLE_EXPECTED!>B()<!> /= 1
  <!VARIABLE_EXPECTED!>B()<!> %= 1
}