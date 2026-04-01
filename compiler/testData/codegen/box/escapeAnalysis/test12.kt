// TODO: check mentioned debug output of escape analyser

class A(val s: String)

// ----- Agressive -----
// PointsTo:
//     RET.v@lue -> P0.inte$tines
// Escapes:
// ----- Passive -----
// PointsTo:
//     RET.v@lue -> P0.inte$tines
// Escapes:
fun foo(arr: Array<A>) = arr[0]

fun box(): String = foo(arrayOf(A("OK"))).s
