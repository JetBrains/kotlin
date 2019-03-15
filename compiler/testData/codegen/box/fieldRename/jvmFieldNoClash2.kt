// TARGET_BACKEND: JVM
// WITH_RUNTIME

class A {
    val x = "outer"
    val y = "outer"

    companion object {
        @JvmField
        val x = "companion"

        const val y = "companion"
    }
}

fun box(): String {
    if (A().x != "outer") return "Fail outer x"
    if (A().y != "outer") return "Fail outer y"
    if (A.x != "companion") return "Fail companion x"
    if (A.y != "companion") return "Fail companion y"

    return "OK"
}
