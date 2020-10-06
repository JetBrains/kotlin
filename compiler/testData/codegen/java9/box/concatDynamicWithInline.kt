// KOTLIN_CONFIGURATION_FLAGS: RUNTIME_STRING_CONCAT=enable
// JVM_TARGET: 9
inline fun test(crossinline s: (String) -> String): String {
    var result = "1" + s("2") + "3" + 4 + {
        "5" + s("6") + "7"
    }()

    result += object  {
        fun run() = "8" + s("9") + "10"
    }.run()

    return result
}

fun box(): String {
    val result = test { it }
    if (result != "12345678910")  return "fail 1: $result"

    val result2 = test { it + "_" }
    return if (result2 != "12_3456_789_10")  "fail 2: $result2" else "OK"
}

fun main() {
    box().let { if (it != "OK") throw AssertionError(it) }
}