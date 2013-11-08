package test

annotation class Ann(i: Int)

Ann(@A 1) class MyClass

// EXPECTED: Ann[i = 1.toInt(): jet.Int]
