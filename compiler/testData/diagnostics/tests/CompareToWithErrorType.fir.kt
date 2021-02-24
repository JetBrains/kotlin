// !WITH_NEW_INFERENCE
fun test() {
  if (<!UNRESOLVED_REFERENCE!>x<!> <!UNRESOLVED_REFERENCE!>><!> 0) {

  }
}
