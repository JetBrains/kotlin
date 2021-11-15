// TARGET_BACKEND: JVM

// WITH_STDLIB

object A {

    @JvmStatic fun test(b: String = "OK") : String {
        return b
    }
}

fun box(): String {

    if (A.test() != "OK") return "fail 1"

    return "OK"
}
