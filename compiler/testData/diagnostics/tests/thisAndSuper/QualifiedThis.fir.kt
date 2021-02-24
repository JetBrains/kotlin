// FILE: f.kt
class A() {
  fun foo() : Unit {
    this@A
    <!UNRESOLVED_LABEL!>this@a<!>
    this
  }

  val x = this@A.foo()
  val y = this.foo()
  val z = foo()
}