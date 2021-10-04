fun box(): String {
    if (true) X::y else null
    return "OK"
}

object X {
    private val y = null
}