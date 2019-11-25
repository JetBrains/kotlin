// IGNORE_BACKEND_FIR: JVM_IR
class C() {
    companion object {
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

