// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: 1.kt

fun box(): String {

    if (A.test() != "OK") return "fail 1"

    return "OK"
}

// FILE: 2.kt

object A {

    @JvmStatic fun test(b: String = "OK") : String {
        return b
    }
}

