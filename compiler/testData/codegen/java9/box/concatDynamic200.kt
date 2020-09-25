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
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z   //200

    return if (result.length != 200)
        "fail: ${result.length}" else "OK"
}

fun main() {
    box().let { if (it != "OK") throw AssertionError(it) }
}
