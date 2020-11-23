// KOTLIN_CONFIGURATION_FLAGS: STRING_CONCAT=indy-with-constants
// JVM_TARGET: 9
class A

inline fun test(s: (String) -> Unit) {
    s("456")
}

fun box(a: String, b: String?) {
    val s = a + "1" + "2" + 3 + 4L + b + 5.0 + 6F + '7' + A()
}

// 1 INVOKEDYNAMIC makeConcatWithConstants
// 0 append
// 0 stringPlus