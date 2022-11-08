// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomBoxingInInlineClasses
// TARGET_BACKEND: JVM_IR

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC5(val x: Int) {
    companion object {
        private val storage = mapOf<Int, IC5>()
        operator fun box(x: Int) = storage.get(x) ?: createInlineClassInstance(x)
    }
}

fun forceBoxing(x: Any?) {}

fun box(): String {
    forceBoxing(IC5(0))
    return "OK"
}