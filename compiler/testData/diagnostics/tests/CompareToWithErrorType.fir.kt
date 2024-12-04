// RUN_PIPELINE_TILL: FRONTEND
fun test() {
  if (<!UNRESOLVED_REFERENCE!>x<!> > 0) {

  }
}
