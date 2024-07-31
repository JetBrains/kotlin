package test

annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Int
)

@Ann(1.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>.plus(1), 1.minus(1.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>), 1.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>.times(1.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>)) class MyClass

// EXPECTED: @Ann(p1 = 2, p2 = 0, p3 = 1)
