// SKIP_TXT

fun foo(x: () -> Int): Int = x()

fun <R> myRun(x: () -> R): R = x()

private val a = foo { <!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>myRun { <!TYPE_MISMATCH!>"OK"<!> }<!> }
private val b: Int = <!TYPE_MISMATCH!>myRun { <!TYPE_MISMATCH!>"OK"<!> }<!>
