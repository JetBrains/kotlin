// RUN_PIPELINE_TILL: SOURCE
fun test() {
  if (<!UNRESOLVED_REFERENCE!>x<!> > 0) {

  }
}
