// WITH_STDLIB
// IGNORE_BACKEND: JVM
// TARGET_BACKEND: JVM
// WORKS_WHEN_VALUE_CLASS

// LANGUAGE: +ValueClasses, +GenericInlineClassParameter
// FILE: JavaClass.java

public class JavaClass {
    public static <T> T id(T x) { return x; }
}

// FILE: test.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcInt<T: Int>(val i: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcLong<T: Long>(val l: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcAny<T: Any?>(val a: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IcOverIc<T: IcInt<Int>>(val o: T)

fun box(): String {
    val i = IcInt(1)
    val l = IcLong(2)
    val a = IcAny("string")
    val o = IcOverIc(IcInt(3))

    val ij = JavaClass.id(i)
    val lj = JavaClass.id(l)
    val aj = JavaClass.id(a)
    val oj = JavaClass.id(o)

    if (ij.i != 1) return "Fail 1"
    if (lj.l != 2L) return "Fail 2"
    if (aj.a != "string") return "Fail 3"
    if (oj.o.i != 3) return "Fail 4"

    return "OK"
}