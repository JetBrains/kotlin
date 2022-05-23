// WITH_STDLIB
// WITH_REFLECT

enum class E {
    OK, NOT_OK
}

operator fun E.getValue(x: Any?, y: Any?): String = name

val s: String by E.OK

fun box(): String {
    assert(::s.getDelegate() == E.OK)
    return s
}