package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

@Ann(1 - 1, 1 - 1, 1 - 1, 1 - 1) class MyClass

// EXPECTED: @Ann(b = 0.toByte(), i = 0, l = 0.toLong(), s = 0.toShort())
