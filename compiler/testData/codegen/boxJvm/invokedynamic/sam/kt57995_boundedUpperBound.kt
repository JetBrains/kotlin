// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// 0 java/lang/invoke/LambdaMetafactory

// The SAM type parameter has a non-trivial upper bound, so the careful
// approximation of the 'in' projection gives up (see KT-51868) and the SAM
// conversion must stay class-based: with an erased-to-Any? instantiated
// signature LambdaMetafactory would reject the call site on JDK 9+.

fun interface Cmp<T : CharSequence> {
    fun compare(a: T, b: T): Int
}

fun <T : CharSequence> foo(comparator: Cmp<in T>, a: T, b: T) = comparator.compare(a, b)

fun box(): String {
    val t = foo<String>({ a, b -> a.length - b.length }, "a", "bb")
    if (t >= 0) return "Fail: t=$t"
    return "OK"
}
