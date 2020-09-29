// KOTLIN_CONFIGURATION_FLAGS: RUNTIME_STRING_CONCAT=enable
// JVM_TARGET: 9
inline class Str(val s: String)
inline class NStr(val s: String?)

fun testStr(s: Str?) = "1$s$s"
fun testNStr(ns: NStr?) = "2$ns$ns"

fun box(): String {

    val test1 = testStr(Str("0"))
    if (test1 != "1Str(s=0)Str(s=0)") return "fail 1: $test1"

    val test2 = testStr(null)
    if (test2 != "1nullnull") return "fail 2: $test2"

    val test3 = testNStr(NStr(null))
    if (test3 != "2NStr(s=null)NStr(s=null)") return "fail 3: $test3"

    val test4 = testNStr(NStr("0"))
    if (test4 != "2NStr(s=0)NStr(s=0)") return "fail 4: $test4"

    val test5 = testNStr(null)
    if (test5 != "2nullnull") return "fail 5: $test5"

    return "OK"
}

fun main() {
    box().let { if (it != "OK") throw AssertionError(it) }
}