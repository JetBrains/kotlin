// WITH_STDLIB
// WITH_REFLECT

operator fun Any?.getValue(x: Any?, y: Any?): String {
    return "OK"
}

val s: String by 1

fun box(): String {
    assert(::s.getDelegate() == 1)
    return s
}