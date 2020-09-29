// KOTLIN_CONFIGURATION_FLAGS: RUNTIME_STRING_CONCAT=indy
// JVM_TARGET: 9
fun box(): String {
    val z = "0"
    val result = z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + //200
            z //201

    return if (result.length != 201)
        "fail: ${result.length}" else "OK"
}

fun main() {
    box().let { if (it != "OK") throw AssertionError(it) }
}
