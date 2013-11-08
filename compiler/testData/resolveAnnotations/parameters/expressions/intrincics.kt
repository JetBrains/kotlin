package test

annotation class Ann(p1: Int,
                     p2: Short,
                     p3: Byte,
                     p4: Int,
                     p5: Int,
                     p6: Int
                     )

Ann(1 or 1, 1 and 1, 1 xor 1, 1 shl 1, 1 shr 1, 1 ushr 1) class MyClass

// EXPECTED: Ann[p1 = 1.toInt(): jet.Int, p2 = 1.toShort(): jet.Short, p3 = 0.toByte(): jet.Byte, p4 = 2.toInt(): jet.Int, p5 = 0.toInt(): jet.Int, p6 = 0.toInt(): jet.Int]
