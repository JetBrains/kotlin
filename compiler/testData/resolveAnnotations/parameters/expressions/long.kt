package test

annotation class Ann(
        val l1: Long,
        val l2: Long,
        val l3: Long
)

@Ann(1 + 1, java.lang.Long.MAX_VALUE + 1 - 1, java.lang.Long.MAX_VALUE - 1) class MyClass

// EXPECTED: @Ann(l1 = 2.toLong(), l2 = 9223372036854775807.toLong(), l3 = 9223372036854775806.toLong())
