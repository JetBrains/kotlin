/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
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