package test

annotation class Ann(i: Int)

@Ann((1 + 2) * 2) class MyClass

// EXPECTED: @Ann(i = 6)
