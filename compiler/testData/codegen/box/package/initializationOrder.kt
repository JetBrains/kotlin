var boxtest_log: String = ""

fun box(): String {
    return if (boxtest_log == "bca") "OK" else boxtest_log
}

val b = log("b")
val c = log("c")
val a = log("a")

fun log(message: String) {
    boxtest_log += message
}
