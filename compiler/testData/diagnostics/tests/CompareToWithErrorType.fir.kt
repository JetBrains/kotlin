// !WITH_NEW_INFERENCE
fun test() {
  if (<!UNRESOLVED_REFERENCE!>x<!> > 0) {

  }
}