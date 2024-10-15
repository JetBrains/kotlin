// RUN_PIPELINE_TILL: SOURCE
fun foo1() : Unit {
  <!NO_THIS!>this<!>
  this<!UNRESOLVED_REFERENCE!>@a<!>
}