/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
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