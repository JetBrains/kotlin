import test.*

fun test1(param: String): String {
    var result = "fail1"
    mfun(param) { a ->
        concat("start") {
            result = doSmth(a).toString()
        }
    }

    return result
}

fun box(): String {
    if (test1("start") != "start") return "fail1"
    if (test1("nostart") != "nostart") return "fail2"

    return "OK"
}