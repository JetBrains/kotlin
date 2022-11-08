// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomBoxingInInlineClasses
// TARGET_BACKEND: JVM_IR

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC5(val x: Int) {
    companion object {
        var field: IC5? = IC5(0)
        var field2  = IC5(1)
        operator fun box(x: Int) = field ?: field2
    }
}

fun forceBoxing(x: Any?) {}

fun box(): String {
    forceBoxing(IC5(0))
    return "OK"
}