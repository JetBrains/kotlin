// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun box(): String? {
    val log = System.getProperty("boxtest.log")
    System.clearProperty("boxtest.log") // test can be run twice
    return if (log == "bca") "OK" else log
}

val b = log("b")
val c = log("c")
val a = log("a")

fun log(message: String) {
    val value = (System.getProperty("boxtest.log") ?: "") + message
    System.setProperty("boxtest.log", value)
}