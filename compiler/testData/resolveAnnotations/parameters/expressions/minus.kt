package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 - 1, 1 - 1, 1 - 1, 1 - 1) class MyClass

// EXPECTED: Ann[b = 0.toByte(): jet.Byte, i = 0.toInt(): jet.Int, l = 0.toLong(): jet.Long, s = 0.toShort(): jet.Short]
