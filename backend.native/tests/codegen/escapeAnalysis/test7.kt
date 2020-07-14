/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.test7

class A(val s: String) {
    var h: String = ""
    var p: C = C()
}
class B {
    var f: A = A("qzz")
}
class C {
    var g: A = A("")
}
class D {
    var o: A = A("")
}

// ----- Agressive -----
// PointsTo:
//     P1.g -> D0
//     P2.f -> D0
//     P4.o -> P1.g
//     P4.o -> P2.f
//     P4.o -> D0
//     RET.v@lue -> D1
//     D0.p -> P1
//     D0.h -> P3
// Escapes: D1
// ----- Passive -----
// PointsTo:
//     P1.g -> D0
//     P2.f -> D0
//     P4.o -> P1.g
//     P4.o -> P2.f
//     P4.o -> D0
//     RET.v@lue -> D1
//     D0.p -> P1
//     D0.h -> P3
// Escapes: D1
fun foo(z: Boolean, c: C, b: B, s: String, d: D) {
    val v = if(z) c.g else b.f
    v.h = s
    d.o = v
    val u = v
    u.p = c
}

fun main() = println(foo(true, C(), B(), "zzz", D()))