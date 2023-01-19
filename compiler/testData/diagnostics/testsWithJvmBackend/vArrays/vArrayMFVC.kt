// WITH_STDLIB
// LANGUAGE: +ValueClasses
// ENABLE_JVM_IR_INLINER
// FIR_IDENTICAL

@JvmInline
value class Point(val x: Int, val y: Int)


inline fun <reified T> singletonVArray(x: T) {
    val x = VArray(1) { x }
}

fun barMFVCInferred() {
    val x = Point(1, 2)
    singletonVArray(x)
}
