package test

annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Int,
        val p4: Int,
        val p5: Int
)

Ann(1 plus 1, 1 minus 1, 1 times 1, 1 div 1, 1 mod 1) class MyClass

// EXPECTED: Ann[p1 = 2.toInt(): jet.Int, p2 = 0.toInt(): jet.Int, p3 = 1.toInt(): jet.Int, p4 = 1.toInt(): jet.Int, p5 = 0.toInt(): jet.Int]
