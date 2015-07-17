fun println(s: String) {
}

fun box(): String {
    val x = println(":Hi!") as Any
    if (x != Unit) return "Fail: $x"
    return "OK"
}
