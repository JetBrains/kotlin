// KOTLIN_CONFIGURATION_FLAGS: RUNTIME_STRING_CONCAT=enable
// JVM_TARGET: 9
class A

inline fun test(s: (String) -> Unit) {
    s("456")
}

fun box(a: String, b: String?) {
    val s = a + "1" + "2" + 3 + 4L + b + 5.0 + 6F + '7' + A() + true + false + 1u

    a.plus(b)
    b?.plus(a)
    val ref1 = a::plus
    val ref2 = b::plus

    test("123"::plus)
}

// unsigned constant 1u processed as argument (last \u0001)

// 1 "\\u00011234\\u00015.06.07\\u0001truefalse\\u0001"
// 6 INVOKEDYNAMIC makeConcatWithConstants
// 0 append
// 0 stringPlus