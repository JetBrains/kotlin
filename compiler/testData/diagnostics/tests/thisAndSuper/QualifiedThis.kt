// FILE: f.kt
class A() {
  fun foo() : Unit {
    <!UNUSED_EXPRESSION!>this@A<!>
    this<!UNRESOLVED_REFERENCE!>@a<!>
    <!UNUSED_EXPRESSION!>this<!>
  }

  val x = this@A.foo()
  val y = this.foo()
  val z = foo()
}