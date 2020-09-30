// KOTLIN_CONFIGURATION_FLAGS: RUNTIME_STRING_CONCAT=enable
// JVM_TARGET: 9
class A

inline class IC(val x: String)

inline fun test(s: (String) -> Unit) {
    s("456")
}

fun box(a: String, b: String?, x: IC?) {
    val p = 3147483648u
    val s = a + "1" + "2" + 3 + 4L + b + 5.0 + 6F + '7' + A() + true + false + 3147483647u + p + x

    a.plus(b)
    b?.plus(a)
    val ref1 = a::plus
    val ref2 = b::plus

    test("123"::plus)
}

// 7 INVOKEDYNAMIC makeConcatWithConstants
// 1 "IC\(x=\\u0001\)"
// 0 append
// 0 stringPlus

// JVM_TEMPLATES
// unsigned constant 3147483647u is processed as argument (\u0001) and it adds additional `UInt.toString-impl` call
// 1 "\\u00011234\\u00015.06.07\\u0001truefalse\\u0001\\u0001\\u0001"
// 2 INVOKESTATIC kotlin/UInt.toString-impl

// Old backend perform inline class boxing...
// 1 INVOKESTATIC IC.box-impl

// one in IC.toString()
// 1 INVOKESTATIC IC.toString-impl

// JVM_IR_TEMPLATES
//  unsigned constant 3147483647u is inlined
// 1 "\\u00011234\\u00015.06.07\\u0001truefalse3147483647\\u0001\\u0001"
// 1 INVOKESTATIC kotlin/UInt.toString-impl

// .. but ir backend performs wise `toString-impl` call
// one in IC.toString() + one in concatenation
// 2 INVOKESTATIC IC.toString-impl