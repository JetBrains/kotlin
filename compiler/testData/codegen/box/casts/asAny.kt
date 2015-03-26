fun println(s: String) {
}

fun box(): String {
    println(":Hi!") as Any
    return "OK"
}
