class C() {
    default object {
        private fun <T> create() = C()
    }

    class ZZZ {
        val c = C.create<String>()
    }
}

fun box(): String {
    C.ZZZ().c
    return "OK"
}

