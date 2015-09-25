package test

annotation class Ann(val c1: Char)

@Ann('a' - 'a') class MyClass

// EXPECTED: @Ann(c1 = 0)
