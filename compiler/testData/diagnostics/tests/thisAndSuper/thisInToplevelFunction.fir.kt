fun foo1() : Unit {
  <!NO_THIS!>this<!>
  this<!UNRESOLVED_LABEL!>@a<!>
}
