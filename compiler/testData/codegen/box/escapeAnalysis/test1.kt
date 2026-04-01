// TODO: check mentioned debug output of escape analyser

class A(val s: String)

// ----- Agressive -----
// PointsTo:
//     RET.v@lue -> P0
// Escapes:
// ----- Passive -----
// PointsTo:
//     RET.v@lue -> P0
// Escapes:
fun foo(a: A) = a

fun box(): String = foo(A("OK")).s
