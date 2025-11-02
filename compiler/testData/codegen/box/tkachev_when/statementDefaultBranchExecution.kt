fun foo(i: Int): String {
    var x = ""
    when (i) {
        0 -> x = "Zero"
        10 -> x = "Ten"
        else -> x = "OK"
    }
    return x
}

fun box(): String {
    return foo(50)
}