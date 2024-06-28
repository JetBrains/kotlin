// TARGET_BACKEND: JVM

// WITH_REFLECT

class Eq {
    override fun equals(other: Any?): Boolean = true
    override fun toString(): String = "1"
    val x: String get() = "2"
}

fun box(): String {
    require(Eq()::toString.callBy(mapOf()) == "1")
    require(Eq()::x.callBy(mapOf()) == "2")
    return "OK"
}
