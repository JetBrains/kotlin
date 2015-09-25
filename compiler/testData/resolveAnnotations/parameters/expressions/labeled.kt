package test

annotation class Ann(i: Double)

@Ann(A@ 1.0) class MyClass

// EXPECTED: @Ann(i = 1.0.toDouble())
