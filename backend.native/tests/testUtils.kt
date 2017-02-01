package kotlin.test

class TestFailedException(val msg:String):RuntimeException(msg)

fun <T> assertEquals(a:T, b:T, msg:String) { if (a != b) throw TestFailedException(msg) }