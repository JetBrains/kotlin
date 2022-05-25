// WITH_STDLIB
// CHECK_BYTECODE_LISTING

class C {
    val s: String by this
}

operator fun C.getValue(x: Any?, y: Any?): String {
    return "OK"
}

fun box() = C().s