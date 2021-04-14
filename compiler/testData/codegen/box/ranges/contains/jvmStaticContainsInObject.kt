// TARGET_BACKEND: JVM
// WITH_RUNTIME

object O {
    @JvmStatic
    operator fun contains(x: String): Boolean = x == "O"
}

fun box() =
    if ("O" in O) "OK" else "Failed"
