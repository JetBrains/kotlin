// KOTLIN_CONFIGURATION_FLAGS: STRING_CONCAT=indy
// JVM_TARGET: 9
fun test(z: Long): String {
    val result = "" + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z   //200 z

    return result
}

fun box(): String {
    val result = test(0L)

    if (result.length != 200)
        return "fail 1: ${result.length}"

    return if (result != "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000")
        "fail 2: ${result}"
    else "OK"
}