// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value class A(val a: B?, val i: Int)

value class B(val b: A)

fun box(): String {
    val a = A(B(A(null, 1)), 2)
    if (a.a?.b?.i != 1 || a.i != 2) return "Fail: $a"
    if (a != A(B(A(null, 1)), 2)) return "Fail: equals"
    return "OK"
}
