// TARGET_BACKEND: JVM
// WITH_REFLECT

enum class E {
    OK, NOT_OK
}

operator fun E.getValue(x: Any?, y: Any?): String = name

val s: String by E.OK

fun box() = if (::s.getDelegate() == E.OK) "OK" else "FAILURE"
