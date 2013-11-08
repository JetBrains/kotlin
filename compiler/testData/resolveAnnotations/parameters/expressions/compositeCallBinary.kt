package test

annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Int
)

Ann(1.toInt().plus(1), 1.minus(1.toInt()), 1.toInt().times(1.toInt())) class MyClass

// EXPECTED: Ann[p1 = 2.toInt(): jet.Int, p2 = 0.toInt(): jet.Int, p3 = 1.toInt(): jet.Int]
