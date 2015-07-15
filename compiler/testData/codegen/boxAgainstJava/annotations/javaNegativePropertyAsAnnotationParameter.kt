Ann(Foo.i, Foo.s, Foo.f, Foo.d, Foo.l, Foo.b) class MyClass

fun box(): String {
    val ann = javaClass<MyClass>().getAnnotation(javaClass<Ann>())
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.i != -2) return "fail: annotation parameter i should be -2, but was ${ann.i}"
    if (ann.s != (-2).toShort()) return "fail: annotation parameter i should be -2, but was ${ann.i}"
    if (ann.f != -2.toFloat()) return "fail: annotation parameter i should be -2, but was ${ann.i}"
    if (ann.d != -2.toDouble()) return "fail: annotation parameter i should be -2, but was ${ann.i}"
    if (ann.l != -2.toLong()) return "fail: annotation parameter i should be -2, but was ${ann.i}"
    if (ann.b != (-2).toByte()) return "fail: annotation parameter i should be -2, but was ${ann.i}"
    return "OK"
}

annotation(retention = AnnotationRetention.RUNTIME) class Ann(
        val i: Int,
        val s: Short,
        val f: Float,
        val d: Double,
        val l: Long,
        val b: Byte
)