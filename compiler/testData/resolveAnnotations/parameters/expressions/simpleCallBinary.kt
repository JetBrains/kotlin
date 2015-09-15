package test

annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Int,
        val p4: Int,
        val p5: Int
)

@Ann(1.plus(1), 1.minus(1), 1.times(1), 1.div(1), 1.mod(1)) class MyClass

// EXPECTED: @Ann(p1 = 2, p2 = 0, p3 = 1, p4 = 1, p5 = 0)
