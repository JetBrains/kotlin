// TARGET_BACKEND: JVM

// WITH_STDLIB

@Ann(Foo.i, Foo.s, Foo.f, Foo.d, Foo.l, Foo.b, Foo.bool, Foo.c, Foo.str) class MyClass

fun box(): String {
    val ann = MyClass::class.java.getAnnotation(Ann::class.java)
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.i != 2) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.s != 2.toShort()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.f != 2.toFloat()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.d != 2.toDouble()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.l != 2.toLong()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.b != 2.toByte()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (!ann.bool) return "fail: annotation parameter i should be true, but was ${ann.i}"
    if (ann.c != 'c') return "fail: annotation parameter i should be c, but was ${ann.i}"
    if (ann.str != "str") return "fail: annotation parameter i should be str, but was ${ann.i}"
    return "OK"
}

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(
        val i: Int,
        val s: Short,
        val f: Float,
        val d: Double,
        val l: Long,
        val b: Byte,
        val bool: Boolean,
        val c: Char,
        val str: String
)

class Foo {
    companion object {
        const val i: Int = 2
        const val s: Short = 2
        const val f: Float = 2.0.toFloat()
        const val d: Double = 2.0
        const val l: Long = 2
        const val b: Byte = 2
        const val bool: Boolean = true
        const val c: Char = 'c'
        const val str: String = "str"
    }
}
