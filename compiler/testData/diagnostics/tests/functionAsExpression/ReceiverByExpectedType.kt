// !WITH_NEW_INFERENCE
fun foo(<!UNUSED_PARAMETER!>f<!>: String.() -> Int) {}
val test = foo(<!OI;TYPE_MISMATCH!>fun () = <!UNRESOLVED_REFERENCE!>length<!><!>)