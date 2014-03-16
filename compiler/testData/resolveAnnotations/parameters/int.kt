package test

annotation class Ann(
        val b1: Int,
        val b2: Int,
        val b3: Int,
        val b4: Int
)

Ann(1, 1.toInt(), 2147483648.toInt(), 2147483648) class MyClass

// EXPECTED: Ann(b1 = IntegerValueType(1): IntegerValueType(1), b2 = 1: Int, b3 = -2147483648: Int, b4 = IntegerValueType(2147483648): IntegerValueType(2147483648))