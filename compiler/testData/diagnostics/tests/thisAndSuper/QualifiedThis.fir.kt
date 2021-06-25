// FILE: f.kt
class A() {
  fun foo() : Unit {
    this@A
    this<!UNRESOLVED_LABEL!>@a<!>
    this
  }

  val x = this@A.foo()
  val y = this.foo()
  val z = foo()
}
