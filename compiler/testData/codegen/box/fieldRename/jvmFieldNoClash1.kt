// TARGET_BACKEND: JVM
// WITH_STDLIB

class A {
    @JvmField
    val x = "outer"

    companion object {
        val x = "companion"
    }
}

fun box(): String {
    if (A().x != "outer") return "Fail outer"
    if (A.x != "companion") return "Fail companion"

    return "OK"
}
