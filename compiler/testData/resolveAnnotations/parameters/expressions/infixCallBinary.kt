package test

annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Int,
        val p4: Int,
        val p5: Int
)

Ann(1 plus 1, 1 minus 1, 1 times 1, 1 div 1, 1 mod 1) class MyClass

// EXPECTED: Ann[p1 = IntegerValueType(2): IntegerValueType(2), p2 = IntegerValueType(0): IntegerValueType(0), p3 = IntegerValueType(1): IntegerValueType(1), p4 = IntegerValueType(1): IntegerValueType(1), p5 = IntegerValueType(0): IntegerValueType(0)]
