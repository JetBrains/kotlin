// FILE: f.kt
class A() {
  fun foo() : Unit {
    <!UNUSED_EXPRESSION!>this@A<!>
    this<!UNRESOLVED_REFERENCE!>@a<!>
    <!UNUSED_EXPRESSION!>this<!>
  }

  val x = this@A.<!DEBUG_INFO_LEAKING_THIS!>foo<!>()
  val y = this.<!DEBUG_INFO_LEAKING_THIS!>foo<!>()
  val z = <!DEBUG_INFO_LEAKING_THIS!>foo<!>()
}