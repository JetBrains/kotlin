// WITH_STDLIB
// CHECK_BYTECODE_LISTING

class C {
    operator fun getValue(x: Any?, y: Any?): String {
        return "OK"
    }


    val s: String by this
}

fun box() = C().s