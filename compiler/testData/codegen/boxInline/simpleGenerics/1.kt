import test.*

fun test1(s: Long): String {
    return doSmth(s)
}

fun box(): String {
    val result = test1(11.toLong())
    if (result != "11") return "fail1: ${result}"

    return "OK"
}