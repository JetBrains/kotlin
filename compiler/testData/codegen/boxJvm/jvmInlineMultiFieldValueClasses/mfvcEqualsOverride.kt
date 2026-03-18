// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

import kotlin.math.abs

interface I {
    fun equals(param: MFVC): Boolean
}

@JvmInline
value class MFVC(val value: Int, val y: Int) : I {
    override fun equals(param: MFVC): Boolean {
        return abs(value - param.value) < 2
    }
}

fun box(): String {
    val a1Typed: MFVC = MFVC(1, 2)
    val b1Typed: MFVC = MFVC(2, 3)
    val c1Typed: MFVC = MFVC(3, 4)
    val a1Untyped: I = a1Typed
    val b1Untyped: I = b1Typed
    val c1Untyped: I = c1Typed

    require(a1Typed == a1Typed && a1Untyped == a1Untyped)
    require(a1Typed == b1Typed && a1Untyped == b1Untyped)
    require(a1Typed != c1Typed && a1Untyped != c1Untyped)
    require(b1Typed == a1Typed && b1Untyped == a1Untyped)
    require(b1Typed == b1Typed && b1Untyped == b1Untyped)
    require(b1Typed == c1Typed && b1Untyped == c1Untyped)
    require(c1Typed != a1Typed && c1Untyped != a1Untyped)
    require(c1Typed == b1Typed && c1Untyped == b1Untyped)
    require(c1Typed == c1Typed && c1Untyped == c1Untyped)
    
    return "OK"
}
