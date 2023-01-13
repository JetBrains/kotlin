package test

annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Long,
        val p4: Long,
        val p5: Int
)

@Ann(
    p1 = java.lang.Integer.MAX_VALUE + 1,
    p2 = 1 + 1,
    p3 = <!ARGUMENT_TYPE_MISMATCH!>java.lang.Integer.MAX_VALUE + 1<!>,
    p4 = <!ARGUMENT_TYPE_MISMATCH!>1.toInt() + 1.toInt()<!>,
    p5 = 1.toInt() + 1.toInt()
) class MyClass

// EXPECTED: @Ann(p1 = -2147483648, p2 = 2, p3 = -2147483648, p4 = 2, p5 = 2)
