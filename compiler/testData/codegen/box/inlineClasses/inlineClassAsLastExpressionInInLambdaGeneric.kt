// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter
// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt
inline fun <R> inlinedRun(block: () -> R): R = block()

// FILE: main.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class Name<T: String>(private val value: T) {
    fun asValue(): String = value
}

fun <T: String> concat(a: Name<T>, b: Name<T>) = a.asValue() + b.asValue()

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(private val value: T) {
    fun asValue(): Int = value
}

fun box(): String {
    val o = inlinedRun {
        Name("O")
    }

    val k = notInlinedRun {
        Name("K")
    }

    if (concat(o, k) != "OK") return "fail 1"

    val a = UInt(1)
    val one = inlinedRun {
        a
    }

    if (one.asValue() != 1) return "fail 2"

    val b = UInt(2)
    val two = notInlinedRun {
        b
    }

    if (two.asValue() != 2) return "fail 3"

    return "OK"
}

fun <R> notInlinedRun(block: () -> R): R = block()