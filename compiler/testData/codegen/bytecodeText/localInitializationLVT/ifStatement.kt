// IGNORE_BACKEND: JVM_IR
// TODO KT-36812 Generate proper lifetime intervals for local variables in JVM_IR

import kotlin.random.Random

fun test(): Char {
    val c: Char
    if (Random.nextBoolean()) {
        c = '1'
    } else {
        c = '2'
    }
    return c
}

// 3 ISTORE 0
// 1 LOCALVARIABLE c C L1 L7 0
