// TODO: check mentioned debug output of escape analyser

class A(val s: String)
class B {
    var f: A = A("qzz")
}
class C {
    var g: B = B()
}

// ----- Agressive -----
// PointsTo:
//     P0.g.f -> P1.g.f
//     RET.v@lue -> D0
// Escapes: D0
// ----- Passive -----
// PointsTo:
//     P0.g.f -> P1.g.f
//     RET.v@lue -> D0
// Escapes: D0
fun foo(c1: C, c2: C) {
    c1.g.f = c2.g.f
}

fun box(): String {
    foo(C(), C())
    return "OK"
}
