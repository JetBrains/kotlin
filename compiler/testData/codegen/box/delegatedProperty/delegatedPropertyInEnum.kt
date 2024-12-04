// IGNORE_BACKEND: JVM

object D {
    operator fun getValue(a: Any?, b: Any?): String = "OK"
}

enum class A {
    GOO;
    val a by D
    val b = a
}

fun box() = A.GOO.b
