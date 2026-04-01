// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// FILE: Foo.java

class Foo {
    public static final int i = 2;
    public static final short s = 2;
    public static final float f = 2;
    public static final double d = 2;
    public static final long l = 2;
    public static final byte b = 2;
    public static final char c = 99;
}

// MODULE: main(lib)
// FILE: 1.kt

@Ann(Foo.i, Foo.s, Foo.f, Foo.d, Foo.l, Foo.b, Foo.c) class MyClass

fun box(): String {
    val ann = MyClass::class.java.getAnnotation(Ann::class.java)
    if (ann == null) return "fail: cannot find Ann on MyClass"
    if (ann.i != 2) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.s != 2.toShort()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.f != 2.toFloat()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.d != 2.toDouble()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.l != 2.toLong()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.b != 2.toByte()) return "fail: annotation parameter i should be 2, but was ${ann.i}"
    if (ann.c != 'c') return "fail: annotation parameter i should be c, but was ${ann.i}"
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
        val c: Char
)
