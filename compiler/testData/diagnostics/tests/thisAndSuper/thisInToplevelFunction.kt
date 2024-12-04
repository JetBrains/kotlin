// RUN_PIPELINE_TILL: FRONTEND
fun foo1() : Unit {
  <!NO_THIS!>this<!>
  this<!UNRESOLVED_REFERENCE!>@a<!>
}