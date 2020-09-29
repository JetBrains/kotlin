// KOTLIN_CONFIGURATION_FLAGS: RUNTIME_STRING_CONCAT=enable
// JVM_TARGET: 9
fun box(): String {
    val p = 3147483648u
    val a = "_"
    val b = "_"
    val s =  a + "1" + "2" + 3 + 4L + b + 5.0 + 6F + '7' + true + false + 3147483647u + p

    return if (s != "_1234_5.06.07truefalse31474836473147483648") "fail $s" else "OK"
}

fun main() {
    box().let { if (it != "OK") throw AssertionError(it) }
}
