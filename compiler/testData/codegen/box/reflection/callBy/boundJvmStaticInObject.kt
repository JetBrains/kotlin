// TARGET_BACKEND: JVM

// WITH_REFLECT

object Host {
    @JvmStatic fun concat(s1: String, s2: String, s3: String = "K", s4: String = "x") =
            s1 + s2 + s3 + s4
}

fun box(): String {
    val concat = Host::concat
    val concatParams = concat.parameters
    return concat.callBy(mapOf(
            concatParams[0] to "",
            concatParams[1] to "O",
            concatParams[3] to ""
    ))
}
