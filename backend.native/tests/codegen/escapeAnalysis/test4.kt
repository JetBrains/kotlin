/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.test4

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

fun main() = println(foo(C(), C()))