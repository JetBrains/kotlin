package test

annotation class Ann(
        val b1: Int,
        val b2: Int,
        val b3: Int,
        val b4: Int
)

@Ann(1, 1.toInt(), 2147483648.toInt(), 2147483648) class MyClass

// EXPECTED: @Ann(b1 = 1, b2 = 1, b3 = -2147483648, b4 = 2147483648.toLong())