// SKIP_TXT

fun foo(x: () -> Int): Int = x()

fun <R> myRun(x: () -> R): R = x()

private val a = foo { myRun { <!ARGUMENT_TYPE_MISMATCH!>"OK"<!> } }
private val b: Int = myRun { <!ARGUMENT_TYPE_MISMATCH!>"OK"<!> }
