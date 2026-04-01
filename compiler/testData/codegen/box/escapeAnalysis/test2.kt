// TODO: check mentioned debug output of escape analyser

class A(val s: String)

// ----- Agressive -----
// PointsTo:
//     RET.v@lue -> P0.s
// Escapes:
// ----- Passive -----
// PointsTo:
//     RET.v@lue -> P0.s
// Escapes:
fun foo(a: A) = a.s

fun box(): String = foo(A("OK"))
