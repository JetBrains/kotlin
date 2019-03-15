// TARGET_BACKEND: JVM

// WITH_RUNTIME

object A {

    @JvmStatic inline fun test(b: String = "OK") : String {
        return b
    }
}

fun box(): String {

    if (A.test() != "OK") return "fail 1"

    return "OK"
}
