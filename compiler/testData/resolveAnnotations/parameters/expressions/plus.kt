package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 + 1, 1 + 1, 1 + 1, 1 + 1) class MyClass

// EXPECTED: Ann[b = 2.toByte(): jet.Byte, i = 2.toInt(): jet.Int, l = 2.toLong(): jet.Long, s = 2.toShort(): jet.Short]
