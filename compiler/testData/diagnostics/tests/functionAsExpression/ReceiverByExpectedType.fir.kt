// !WITH_NEW_INFERENCE
fun foo(f: String.() -> Int) {}
val test = foo(fun () = <!ARGUMENT_TYPE_MISMATCH, UNRESOLVED_REFERENCE!>length<!>)
