typealias SuspendFn = suspend () -> Unit

val test1f: suspend () -> Unit = <!INITIALIZER_TYPE_MISMATCH!>fun () {}<!>
val test2f: suspend Any.() -> Unit = <!INITIALIZER_TYPE_MISMATCH!>fun Any.() {}<!>

// This is a bug in the old inference and should be fixed in new inference
// see "Fix anonymous function literals handling in type checker" for more details
val test3f: suspend Any.(Int) -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (k: Int) = k + 1<!>
val test4f: SuspendFn = <!INITIALIZER_TYPE_MISMATCH!>fun Any.() {}<!>
