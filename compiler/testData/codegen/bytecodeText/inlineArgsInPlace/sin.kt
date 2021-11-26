// WITH_STDLIB

import kotlin.math.sin

fun test(x: Double) = sin(2 * x)

// JVM_IR_TEMPLATES:
// 0 DSTORE
// 1 DLOAD
