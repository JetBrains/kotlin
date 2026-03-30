var log = ""

data class Args(val a: String, val b: String)

fun spread(): Args {
    log += "S"
    return Args("A", "B")
}

fun tail(): String {
    log += "T"
    return "C"
}

fun foo(a: String, b: String, c: String): String {
    return a + b + c
}

fun box(): String {
    val result = foo(...spread(), c = tail())
    return if (result == "ABC" && log == "ST") "OK" else "fail: $result|$log"
}
