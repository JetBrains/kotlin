// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomBoxingInInlineClasses
// TARGET_BACKEND: JVM_IR

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC1(val x: Int) {
    companion object {
        operator fun box(x: Int) = boxByDefault<IC1>(42)
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2(val x: IC1) {
    companion object {
        operator fun box(x: IC1) = boxByDefault<IC2>(forceBoxing(x))
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC3(val x: Int) {
    companion object {
        private val hotValues = mapOf<Int, IC3>(0 to boxByDefault(0), 1 to boxByDefault(1), 2 to boxByDefault(2))
        operator fun box(x: Int): IC3 {
            return (hotValues?.get(x)
                ?: (boxByDefault<IC3>(x) as IC3?))!! // the weird cast to nullable is a temporary solution to avoid redundant boxing
        }
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC4(val x: Int) {
    companion object {
        private val cache = mutableMapOf<Int, Boxed<IC4>>()
        operator fun box(x: Int) = (cache.myComputeIfAbsent(x) { Boxed(boxByDefault(x)) }).value
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC5(val x: Int) {
    companion object {
        operator fun box(x: Int): Nothing = TODO()
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC6(val x: Int) {
    companion object {
        private val hotValues = mapOf<Int, Boxed<IC6>>(
            0 to Boxed(boxByDefault(0)),
            1 to Boxed(boxByDefault(1)),
            2 to Boxed(boxByDefault(2))
        )

        operator fun box(x: Int): IC6 {
            return (hotValues?.get(x) ?: Boxed(boxByDefault(x))).value
        }
    }
}

inline fun <reified T> assertThrows(block: () -> Unit): Boolean {
    try {
        block.invoke()
    } catch (t: Throwable) {
        return t is T
    }
    return false
}


fun <T, R> MutableMap<T, R>.myComputeIfAbsent(key: T, computation: () -> R): R {
    if (containsKey(key)) return this[key]!!
    val value = computation()
    this[key] = value
    return value
}

fun <T> forceBoxing(x: T): T = x
fun refEqualsBoxed(x: Any?, y: Any?) = x === y
class Boxed<T>(val value: T)

fun box(): String {
    if (forceBoxing(IC1(0)).x != 42) return "Fail 1"

    if (forceBoxing(IC2(IC1(0))).x.x != 42) return "Fail 2"

    if (!refEqualsBoxed(IC3(0), IC3(0))) return "Fail 3.1"
    if (refEqualsBoxed(IC3(5), IC3(5))) return "Fail 3.2"

    if (!refEqualsBoxed(IC4(0), IC4(0))) return "Fail 4.1"
    if (!refEqualsBoxed(IC4(42), IC4(42))) return "Fail 4.2"

    if (!assertThrows<NotImplementedError> { forceBoxing(IC5(0)) }) return "Fail 5"

    if (!refEqualsBoxed(IC4(5), IC4(5))) return "Fail 6"

    return "OK"
}