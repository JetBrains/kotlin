// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value class A(val a: B?, val i: Int)

value class B(val b: A)

value class Nat(val previous: Nat?)

fun Nat?.toInt(): Int = if (this == null) 0 else 1 + previous.toInt()

inline value class InlineToSingle(val s: SingleFromInline)

value class SingleFromInline(val i: InlineToSingle?)

fun box(): String {
    val a = A(B(A(null, 1)), 2)
    if (a.a?.b?.i != 1 || a.i != 2) return "Fail: $a"
    if (a != A(B(A(null, 1)), 2)) return "Fail: equals"
    val three = Nat(Nat(Nat(null)))
    if (three.toInt() != 3) return "Fail: Nat"
    if (three != Nat(Nat(Nat(null)))) return "Fail: Nat equals"
    val s = SingleFromInline(InlineToSingle(SingleFromInline(null)))
    if (s.i?.s?.i != null) return "Fail: $s"
    if (s != SingleFromInline(InlineToSingle(SingleFromInline(null)))) return "Fail: SingleFromInline equals"
    return "OK"
}
