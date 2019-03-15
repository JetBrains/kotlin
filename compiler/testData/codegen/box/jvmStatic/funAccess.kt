// TARGET_BACKEND: JVM

// WITH_RUNTIME

var holder = ""

fun getA(): A {
    holder += "OK"
    return A
}

object A {
    @JvmStatic fun a(): String {
        return holder
    }
}

fun box(): String {
    return getA().a()
}
