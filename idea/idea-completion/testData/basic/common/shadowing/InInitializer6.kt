fun foo(p: () -> Unit): String = ""

val xxx: String = foo { <caret> }

// EXIST: xxx
