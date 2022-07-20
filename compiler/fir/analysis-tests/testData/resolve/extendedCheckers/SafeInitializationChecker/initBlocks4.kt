class B(bool: Boolean) {
    var a: String
    var b: String

    init {
        if (bool) {
            a = "Hello"
            if (bool) b = "" else b = a
        } else a = ""

        b = a
    }
}