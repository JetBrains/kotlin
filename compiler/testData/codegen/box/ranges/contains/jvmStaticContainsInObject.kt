// TARGET_BACKEND: JVM
// WITH_STDLIB

object O {
    @JvmStatic
    operator fun contains(x: String): Boolean = x == "O"
}

fun box() =
    if ("O" in O) "OK" else "Failed"
