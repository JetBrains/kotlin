// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
val la = { a -> }
val las = { a: Int -> }

val larg = <!UNRESOLVED_REFERENCE!>{ a -> }(123)<!>
val twoarg = <!UNRESOLVED_REFERENCE!>{ a, b: String, c -> }(123, "asdf", 123)<!>