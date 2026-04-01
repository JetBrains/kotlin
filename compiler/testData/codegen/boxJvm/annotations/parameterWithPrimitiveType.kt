// TARGET_BACKEND: JVM

// WITH_STDLIB

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val f: Float,
        val d: Double,
        val l: Long,
        val c: Char,
        val bool: Boolean
)

fun box(): String {
    val ann = MyClass::class.java.getAnnotation(Ann::class.java)
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.b != 1.toByte()) return "fail: annotation parameter b should be 1, but was ${ann.b}"
    if (ann.s != 1.toShort()) return "fail: annotation parameter s should be 1, but was ${ann.s}"
    if (ann.i != 1) return "fail: annotation parameter i should be 1, but was ${ann.i}"
    if (ann.f != 1.toFloat()) return "fail: annotation parameter f should be 1, but was ${ann.f}"
    if (ann.d != 1.0) return "fail: annotation parameter d should be 1, but was ${ann.d}"
    if (ann.l != 1.toLong()) return "fail: annotation parameter l should be 1, but was ${ann.l}"
    if (ann.c != 'c') return "fail: annotation parameter c should be 1, but was ${ann.c}"
    if (!ann.bool) return "fail: annotation parameter bool should be 1, but was ${ann.bool}"
    return "OK"
}

@Ann(1, 1, 1, 1.0.toFloat(), 1.0, 1, 'c', true) class MyClass
