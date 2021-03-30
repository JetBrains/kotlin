// TARGET_BACKEND: JVM_IR
// DUMP_IR
// WITH_STDLIB

var result = 0

fun takeString(s: String) {
    s.split("\n").forEach {
        result += it.toIntOrNull() ?: 0
    }
}

fun test() {
    val x = 10
    val y = 10

    fun localFunc() {
        for (i in 0..x) {
            val s = buildString {
                for (j in 0..y) {
                    appendLine("${i * j}")
                }
            }
            takeString(s)
        }
    }

    localFunc()
}

fun box(): String {
    test()
    return if (result == 3025) "OK" else "Fail: $result"
}
