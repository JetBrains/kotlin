// TODO: check mentioned debug output of escape analyser
// WITH_STDLIB
import kotlin.test.*

class G(val x: Int)

class F(val s: String) {
    var g = G(0)
}

class A {
    var f = F("")
}

// ----- Agressive -----
// PointsTo:
//     P0.f -> D0
//     RET.v@lue -> D0
// Escapes:
// ----- Passive -----
// PointsTo:
//     P0.f -> D0
//     RET.v@lue -> D0
// Escapes: D0
fun foo(a: A): F {
    val v = F("zzz")
    a.f = v
    return v
}

fun bar(): F {
    val w = A()
    val u = foo(w)
    w.f.g = G(42)
    return u
}

fun box(): String {
    assertEquals(42, bar().g.x)
    return "OK"
}
