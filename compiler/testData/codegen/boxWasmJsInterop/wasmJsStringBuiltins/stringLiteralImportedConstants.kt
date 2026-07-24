// TARGET_BACKEND: WASM
// WITH_STDLIB

// FILE: literalsA.kt

const val EDGE_A = "A'\u0000B\uD83D\uDE80e\u0301"

fun literalA(): String = EDGE_A

fun decoratedA(): String = "<<" + EDGE_A + ">>"

// FILE: literalsB.kt

fun literalB(): String = "A'\u0000B\uD83D\uDE80e\u0301"

fun slicedB(): String = ("--" + literalB() + "--").substring(2, 10)

// FILE: main.kt

@JsFun("""(s) => typeof s === "string" && !(s instanceof String)""")
external fun jsIsPrimitiveString(s: String): Boolean

@JsFun("(s) => s === \"A'\\u0000B\\uD83D\\uDE80e\\u0301\"")
external fun jsEqualsEdgeLiteral(s: String): Boolean

fun box(): String {
    val a = literalA()
    val b = literalB()
    val sliced = slicedB()

    if (a != EDGE_A) return "Fail literalA"
    if (b != EDGE_A) return "Fail literalB"
    if (sliced != EDGE_A) return "Fail slicedB: <$sliced>"
    if (decoratedA() != "<<A'\u0000B\uD83D\uDE80e\u0301>>") return "Fail decoratedA"

    for ((index, value) in listOf(a, b, sliced).withIndex()) {
        if (!jsIsPrimitiveString(value)) return "Fail primitive $index"
        if (!jsEqualsEdgeLiteral(value)) return "Fail JS equality $index"
    }

    return "OK"
}
