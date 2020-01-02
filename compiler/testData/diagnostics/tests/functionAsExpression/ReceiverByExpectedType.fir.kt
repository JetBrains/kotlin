// !WITH_NEW_INFERENCE
fun foo(f: String.() -> Int) {}
val test = foo(fun () = <!UNRESOLVED_REFERENCE!>length<!>)