class C(var s: String)

fun box(): String {
    val c = C("B")

    return when {
        c.s == "A" -> "FAIL1"
        c.s == "B" -> "OK"
        else -> "FAIL2"
    }
}
