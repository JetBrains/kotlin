import test.*

fun test1(s: Long): String {
    var result = "OK"
    result = mfun(s) { a ->
        result + doSmth(s) + doSmth(a)
    }

    return result
}

fun box(): String {
    val result = test1(11.toLong())
    if (result != "OK1111") return "fail1: ${result}"

    return "OK"
}