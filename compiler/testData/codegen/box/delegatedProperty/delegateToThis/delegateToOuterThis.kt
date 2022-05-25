// WITH_STDLIB
// CHECK_BYTECODE_LISTING

class O {
    operator fun getValue(x: Any?, y: Any?): String {
        return "OK"
    }


    inner class I {
        val s: String by this@O
    }
}

fun box() = O().I().s