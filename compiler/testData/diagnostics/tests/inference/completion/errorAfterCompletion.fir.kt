// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT

fun foo(x: () -> Int): Int = x()

fun <R> myRun(x: () -> R): R = x()

private val a = foo { myRun { <!RETURN_TYPE_MISMATCH!>"OK"<!> } }
private val b: Int = myRun { <!RETURN_TYPE_MISMATCH!>"OK"<!> }
