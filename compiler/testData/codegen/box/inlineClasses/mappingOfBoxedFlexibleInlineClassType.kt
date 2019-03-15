// WITH_RUNTIME
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR

// FILE: JavaClass.java

public class JavaClass {
    public static <T> T id(T x) { return x; }
}

// FILE: test.kt

inline class IcInt(val i: Int)
inline class IcLong(val l: Long)
inline class IcAny(val a: Any?)
inline class IcOverIc(val o: IcInt)

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