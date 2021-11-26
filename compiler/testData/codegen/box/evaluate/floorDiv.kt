// !LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
// IGNORE_FIR_DIAGNOSTICS
// TARGET_BACKEND: JVM

// WITH_STDLIB

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

@Ann(10.floorDiv(3), 10.floorDiv(-4), (-10).floorDiv(5), (-10).floorDiv(-6)) class MyClass

fun box(): String {
    val annotation = MyClass::class.java.getAnnotation(Ann::class.java)!!
    if (annotation.b != 3.toByte()) return "fail 1"
    if (annotation.s != (-3).toShort()) return "fail 2"
    if (annotation.i != -2) return "fail 3"
    if (annotation.l != 1L) return "fail 4"
    return "OK"
}
