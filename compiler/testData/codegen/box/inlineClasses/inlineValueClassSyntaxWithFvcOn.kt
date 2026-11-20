// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// CHECK_BYTECODE_TEXT
// LANGUAGE: +FullValueClasses

// MODULE: lib
// FILE: lib.kt

inline value class A(val x: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value inline class B(val s: String)

fun takeA(a: A): Int = a.x
fun takeB(b: B): String = b.s

// 1 box-impl\(I\)LA;
// 1 box-impl\(Ljava/lang/String;\)LB;

// MODULE: main(lib)
// FILE: main.kt

inline value class C(val a: A)

OPTIONAL_JVM_INLINE_ANNOTATION
value inline class D(val b: B)

fun takeC(c: C): Int = takeA(c.a)
fun takeD(d: D): String = takeB(d.b)

fun box(): String {
    val c = C(A(1))
    if (takeC(c) != 1) return "Fail 1"
    if ((c as Any) != C(A(1))) return "Fail 2"
    if (c.toString() != "C(a=A(x=1))") return "Fail 3"
    return takeD(D(B("O"))) + D(B("K")).b.s
}

// 1 box-impl\(I\)LC;
// 1 box-impl\(Ljava/lang/String;\)LD;
