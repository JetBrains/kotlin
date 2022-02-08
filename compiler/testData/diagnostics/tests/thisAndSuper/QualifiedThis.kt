// FILE: f.kt
class A() {
  fun foo() : Unit {
    this@A
    this<!UNRESOLVED_REFERENCE!>@a<!>
    this
  }

  val x = this@A.<!DEBUG_INFO_LEAKING_THIS!>foo<!>()
  val y = this.<!DEBUG_INFO_LEAKING_THIS!>foo<!>()
  val z = <!DEBUG_INFO_LEAKING_THIS!>foo<!>()
}
