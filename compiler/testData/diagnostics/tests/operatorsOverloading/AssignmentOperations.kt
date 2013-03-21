class A {
  fun plusAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
  fun minusAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
  fun timesAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
  fun divAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
  fun modAssign(<!UNUSED_PARAMETER!>x<!>: Int) {}
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
  fun plus(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
  fun minus(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
  fun times(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
  fun div(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
  fun mod(<!UNUSED_PARAMETER!>x<!>: Int): B = B()
}

fun testWrong() {
  <!VARIABLE_EXPECTED!>B()<!> += 1
  <!VARIABLE_EXPECTED!>B()<!> -= 1
  <!VARIABLE_EXPECTED!>B()<!> *= 1
  <!VARIABLE_EXPECTED!>B()<!> /= 1
  <!VARIABLE_EXPECTED!>B()<!> %= 1
}