// TARGET_BACKEND: JVM_IR

fun <T : Int> f(a: T?): String = "OK"

fun box(): String = f(null)