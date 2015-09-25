package test

annotation class Ann(val s1: String)

@Ann(s1 = "a" + 1) class MyClass

// EXPECTED: @Ann(s1 = "a1")
