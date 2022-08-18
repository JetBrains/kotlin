fun box(): String {
    val a = "OK"

    val c = {
        val b = a
        b
    }.invoke()

    return a
}
