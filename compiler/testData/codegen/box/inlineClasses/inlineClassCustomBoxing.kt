// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val x: Int) {
    companion object {
        operator fun box(x: Int) = createInlineClassInstance<IC>(42)
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2(val x: IC) {
    companion object {
        operator fun box(x: IC) = createInlineClassInstance<IC2>(x)
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC3<T : Number>(val x: T) {
    companion object {
        operator fun <R : Number> box(x: R) = createInlineClassInstance<IC3<R>>(x.toInt().toDouble())
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC4<T>(val x: T) {
    companion object {

        private val cache = mutableMapOf<Any?, IC4<*>>()

        operator fun <T> box(x: T): IC4<*> {
            if (!cache.containsKey(x)) cache[x] = createInlineClassInstance(x)
            return cache[x]!!
        }
    }
}

fun foo(a: IC?) = a?.x ?: 0
fun bar(a: IC2?) = a?.x?.x ?: 0

fun box(): String {
    if (foo(IC(1)) != 42) return "Fail 1"
    if (bar(IC2(IC(1))) != 1) return "Fail 2"
    if (setOf(IC3(5.1)).iterator().next().x != 5.0) return "Fail 3"

    val x = setOf(IC4(IC(4))).iterator().next() as Any?
    val y = setOf(IC4(IC(4))).iterator().next() as Any?
    if (!(x === y)) return "Fail 4"

    return "OK"
}