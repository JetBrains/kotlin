// !WITH_NEW_INFERENCE
fun foo(f: String.() -> Int) {}
val test = foo(<!ARGUMENT_TYPE_MISMATCH!>fun () = <!UNRESOLVED_REFERENCE!>length<!><!>)
