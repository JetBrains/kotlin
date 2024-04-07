/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
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
//     P1.g -> D0
//     P2.g -> D0
//     RET.v@lue -> P1.g
//     RET.v@lue -> P2.g
//     RET.v@lue -> D0
//     D0.f -> P3
// Escapes:
// ----- Passive -----
// PointsTo:
//     P1.g -> D0
//     P2.g -> D0
//     RET.v@lue -> P1.g
//     RET.v@lue -> P2.g
//     RET.v@lue -> D0
//     D0.f -> P3
// Escapes:
fun foo(z: Boolean, c1: C, c2: C, a: A): B {
    val v = if(z) c1.g else c2.g
    v.f = a
    return v
}

fun box(): String = foo(true, C(), C(), A("OK")).f.s