// WITH_STDLIB
// WITH_REFLECT
// CHECK_BYTECODE_LISTING

enum class E {
    OK, NOT_OK
}

operator fun E.getValue(x: Any?, y: Any?): String = name

val s: String by E.OK

fun box(): String = s
