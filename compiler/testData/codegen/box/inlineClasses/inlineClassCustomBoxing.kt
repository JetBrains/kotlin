// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomBoxingInInlineClasses
// TARGET_BACKEND: JVM_IR

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val x: Int) {
    companion object {
        operator fun box(x: Int) = boxByDefault<IC>(42)
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2(val x: IC) {
    companion object {
        operator fun box(x: IC) = boxByDefault<IC2>(x)
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC3(val x: String)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC4(val x: String) {
    companion object {

        private val cache = mutableMapOf<String, IC4>()

        operator fun box(x: String): IC4 {
            if (!cache.containsKey(x)) cache[x] = boxByDefault(x)
            return cache[x]!!
        }
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC5(val x: Int) {
    companion object {
        private val storage = mapOf<Int, IC5>(
            0 to boxByDefault(0),
            1 to boxByDefault(1),
            2 to boxByDefault(2)
        )

        operator fun box(x: Int) = storage.getOrElse(x) { boxByDefault(x) }
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC6(val x: Int) {
    companion object {
        operator fun box(x: Int): Nothing = TODO()
    }
}

fun foo(a: IC?) = a?.x ?: 0
fun bar(a: IC2?) = a?.x?.x ?: 0
fun refEquals(a: Any?, b: Any?) = a === b
fun forceBox(x: Any?) {}

inline fun <reified T : Throwable> assertFailsWith(func: () -> Unit): Boolean {
    try {
        func.invoke()
    } catch (t: Throwable) {
        return t is T
    }
    return false
}

fun box(): String {
    if (foo(IC(1)) != 42) return "Fail 1"
    if (bar(IC2(IC(1))) != 1) return "Fail 2"
    if (refEquals(IC3("a"), IC3("a"))) return "Fail 3"
    if (!refEquals(IC4("a"), IC4("a"))) return "Fail 4"
    if (!refEquals(IC5(0), IC5(0))) return "Fail 5"
    if (refEquals(IC5(0), IC5(1))) return "Fail 6"
    //if (refEquals(IC5(5), IC5(5))) return "Fail 7"
    assertFailsWith<NotImplementedError> { forceBox(IC6(0)) }
    return "OK"
}