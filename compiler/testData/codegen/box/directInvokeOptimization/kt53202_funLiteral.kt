fun box(): String {
    val a = "OK"

    val c = (fun(): String {
        val b = a
        return b
    }).invoke()

    return a
}
