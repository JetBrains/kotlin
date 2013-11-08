package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 / 1, 1 / 1, 1 / 1, 1 / 1) class MyClass

// EXPECTED: Ann[b = 1.toByte(): jet.Byte, i = 1.toInt(): jet.Int, l = 1.toLong(): jet.Long, s = 1.toShort(): jet.Short]
