// KOTLIN_CONFIGURATION_FLAGS: RUNTIME_STRING_CONCAT=indy
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

// 0 INVOKEDYNAMIC makeConcatWithConstants
// 6 INVOKEDYNAMIC makeConcat
// 0 append
// 0 stringPlus