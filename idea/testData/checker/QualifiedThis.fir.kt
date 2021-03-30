class Dup {
  fun Dup() : Unit {
    this@Dup
  }
}

class A() {
  fun foo() : Unit {
    this@A
    <error descr="[UNRESOLVED_LABEL] Unresolved label">this@a</error>
    this
  }

  val x = this@A.foo()
  val y = this.foo()
  val z = foo()
}

fun foo1() : Unit {
  <error descr="[NO_THIS] 'this' is not defined in this context">this</error>
  <error descr="[UNRESOLVED_LABEL] Unresolved label">this@a</error>
}
