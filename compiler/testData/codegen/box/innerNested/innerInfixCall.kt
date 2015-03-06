class Z() {

    inner class Z2(val s: String) {

    }

    fun a(): String {
        val s = Z() Z2 "OK"
        return s.s
    }
}

fun box(): String {
     return Z().a()
}
