// WITH_STDLIB
// WITH_REFLECT

operator fun Any?.getValue(x: Any?, y: Any?): String {
    return "OK"
}
const val a = 1

val s: String by a

fun box(): String {
    assert(::s.getDelegate() == 1)
    return s
}