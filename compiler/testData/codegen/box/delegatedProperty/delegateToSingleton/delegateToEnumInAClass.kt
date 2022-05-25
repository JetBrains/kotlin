// WITH_STDLIB
// CHECK_BYTECODE_LISTING

enum class E {
    OK, NOT_OK
}

operator fun E.getValue(x: Any?, y: Any?): String = name

class C {
    val s: String by E.OK
}

fun box(): String = C().s
