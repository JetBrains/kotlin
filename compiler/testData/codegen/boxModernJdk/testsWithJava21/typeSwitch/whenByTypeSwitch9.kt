// TARGET_BACKEND: JVM
// WITH_STDLIB
// WHEN_EXPRESSIONS: INDY
// IGNORE_BACKEND_K1: ANY

// CHECK_BYTECODE_TEXT
// 1 INSTANCEOF
// 1 INVOKEDYNAMIC typeSwitch
// 1 java.lang.Float.class
// 1 java.lang.Double.class
// 1 java.lang.String.class
// 1 java.util.List.class
// 1 int\[\].class
// 1 java.lang.Object\[\].class
// 2 InlineClass.class
// 2 EnumClass.class
// 1 java.lang.Object.class
// 1 kotlin.UInt.class


import kotlin.collections.listOf

@JvmInline
value class InlineClass(private val s: String)

enum class EnumClass {
    A, B, C
}

typealias SomeSet = Set<*>

fun foo(x: Any): Int {
    return when (x) {
        is Float -> 1
        is Double -> 2
        is String -> 3
        is SomeSet -> 4
        is List<*> -> 5
        is IntArray -> 6
        is Array<*> -> 7
        is InlineClass -> 8
        is EnumClass -> 9
        is UInt -> 10
        is Any -> 100
    }
}

fun box(): String {
    if (foo(1.2f) != 1) return "Float"
    if (foo(1.2) != 2) return "Double"
    if (foo("aa") != 3) return "3"
    if (foo(setOf<Int>()) != 4) return "4"
    if (foo(listOf<Int>()) != 5) return "5"
    if (foo(intArrayOf(2, 3)) != 6) return "6"
    if (foo(arrayOf(2, 3)) != 7) return "7"
    if (foo(InlineClass("aa")) != 8) return "8"
    if (foo(EnumClass.A) != 9) return "9"
    if (foo(25u) != 10) return "10"
    if (foo(1) != 100) return "100"

    return "OK"
}