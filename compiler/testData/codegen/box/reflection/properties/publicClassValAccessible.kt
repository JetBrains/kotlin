// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.isAccessible

class Result {
    public val value: String = "OK"
}

fun box(): String {
    val p = Result::value
    p.isAccessible = false
    // setAccessible(false) should have no effect on the accessibility of a public reflection object
    return p.get(Result())
}
