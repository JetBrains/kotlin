package test

annotation class Ann(
        val b1: Long,
        val b2: Long
)

Ann(1, 1.toLong()) class MyClass

// EXPECTED: Ann[b1 = IntegerValueType(1): IntegerValueType(1), b2 = 1.toLong(): jet.Long]